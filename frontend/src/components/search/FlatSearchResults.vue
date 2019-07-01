<template>
<div class="search-result-container">
  <transition name="search-slide">
    <h2 class="search-results-hits" v-if="totalHits > 0">Total hits: {{ totalHits }}</h2>
  </transition>
  <transition-group
    v-infinite-scroll="loadMore"
    infinite-scroll-disabled="mayNotLoad"
    infinite-scroll-distance="100"
    infinite-scroll-listen-for-event="reset-infinite"
    tag="div" class="search-result-list" name="search-slide"
    @after-leave="reset"
    v-if="chapterScope">
    <ChapterSearchResult
      :query="query"
      :result="result"
      :style="{'--delay': `${result.delay}ms`}"
      v-for="(result, index) in results"
      :key="`$route.fullPath` + index">
    </ChapterSearchResult>
  </transition-group>
  <transition-group
    v-infinite-scroll="loadMore"
    infinite-scroll-disabled="mayNotLoad"
    infinite-scroll-distance="100"
    infinite-scroll-listen-for-event="reset-infinite"
    tag="div" class="search-result-list" name="search-slide"
    @after-leave="reset"
    v-else>
    <search-result
      :query="query"
      :result="result"
      :style="{'--delay': `${result.delay}ms`}"
      v-for="(result, index) in results"
      :key="`$route.fullPath` + index">
    </search-result>
  </transition-group>
  <LoadingSpinner v-if="loading"></LoadingSpinner>
  <EmptyCard v-if="noMore && results.length === 0"></EmptyCard>
  <ErrorCard
    :message="errorMessage"
    @retry="reset"
    v-if="errorMessage !== null">
  </ErrorCard>
</div>
</template>

<script>
import axios from 'axios';
import SearchResult from '@/components/search/SearchResult.vue';
import ErrorCard from '@/components/search/ErrorCard.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import EmptyCard from '@/components/search/EmptyCard.vue';
import ChapterSearchResult from '@/components/search/ChapterSearchResult.vue';

export default {
  name: 'flat-search-results',
  components: {
    ChapterSearchResult,
    EmptyCard,
    LoadingSpinner,
    ErrorCard,
    SearchResult,
  },
  props: {
    query: String,
    seriesFilter: Array,
    bookFilter: Array,
    chapterScope: Boolean,
  },
  data() {
    return {
      results: [],
      page: 0,
      totalHits: 0,
      resultDelay: 0,
      lastResult: new Date(),
      errorMessage: null,
      cancelToken: null,
      loading: false,
      noMore: false,
    };
  },
  computed: {
    mayNotLoad() {
      return this.loading || this.noMore || this.errorMessage !== null;
    },
  },
  methods: {
    async reset() {
      this.page = 0;
      this.results = [];
      this.resultDelay = 0;
      this.totalHits = 0;
      this.lastResult = new Date();
      this.loading = false;
      this.noMore = false;
      this.errorMessage = null;
      this.$emit('reset-infinite');
    },
    async loadMore() {
      this.loading = true;
      try {
        if (await this.search()) {
          this.page += 1;
        } else {
          this.noMore = true;
        }
      } catch (error) {
        if (axios.isCancel(error)) {
          return;
        }

        this.errorMessage = this.$getApiError(error);
        if (this.errorMessage === null) {
          this.errorMessage = 'An unknown error has occurred, please report this!';
        }
      }
      this.loading = false;
    },
    async search() {
      if (this.cancelToken !== null) {
        this.cancelToken.cancel();
      }
      this.cancelToken = axios.CancelToken.source();

      if (this.chapterScope) {
        return this.searchChapters();
      }

      return this.searchParagraphs();
    },
    async searchParagraphs() {
      const { data: { results, totalHits } } = await this.$api.post(
        '/books/paragraph-search',
        {
          query: this.query,
          page: this.page,
          seriesFilter: this.seriesFilter,
          bookFilter: this.bookFilter,
        },
        {
          cancelToken: this.cancelToken.token,
        },
      );
      const mapped = results.map(({ book, chapter, paragraphs }) => {
        const mainParagraph = paragraphs.find(p => p.main);

        const resultDate = new Date();
        if ((resultDate.getTime() - this.lastResult.getTime()) <= 500) {
          this.resultDelay += 100;
        } else {
          this.resultDelay = 0;
        }

        this.lastResult = resultDate;

        return {
          book,
          chapter,
          mainParagraph,
          prevParagraphs: paragraphs.filter(p => p.position < mainParagraph.position),
          nextParagraphs: paragraphs.filter(p => p.position > mainParagraph.position),
          showSiblings: false,
          delay: this.resultDelay,
        };
      });

      this.results.push(
        ...mapped,
      );
      this.totalHits = totalHits;

      return mapped.length > 0;
    },
    async searchChapters() {
      const { data: { results, totalHits } } = await this.$api.post(
        '/books/chapter-search',
        {
          query: this.query,
          page: this.page,
          seriesFilter: this.seriesFilter,
          bookFilter: this.bookFilter,
        },
        {
          cancelToken: this.cancelToken.token,
        },
      );
      const mapped = results.map(({ book, chapter }) => {
        const resultDate = new Date();
        if ((resultDate.getTime() - this.lastResult.getTime()) <= 500) {
          this.resultDelay += 100;
        } else {
          this.resultDelay = 0;
        }

        this.lastResult = resultDate;

        return {
          book,
          chapter,
          delay: this.resultDelay,
        };
      });

      this.results.push(
        ...mapped,
      );
      this.totalHits = totalHits;

      return mapped.length > 0;
    },
  },
  watch: {
    query() {
      this.reset();
    },
    seriesFilter() {
      this.reset();
    },
    bookFilter() {
      this.reset();
    },
    chapterScope() {
      this.reset();
    },
  },
};
</script>

<style lang="scss">
.search-result-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  position: relative;

  .search-result:first-child {
    margin-top: 0.25rem;
  }
}
</style>

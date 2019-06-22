<template>
<div class="search-result-container">
  <transition name="search-slide">
    <h2 v-if="totalHits > 0">Total hits: {{ totalHits }}</h2>
  </transition>
  <transition-group tag="div" class="search-result-list" name="search-slide"
                    @after-leave="infiniteId = (new Date()).getTime()">
    <search-result
      :result="result"
      :style="{'--delay': `${result.delay}ms`}"
      v-for="(result, index) in results"
      :key="`$route.fullPath` + index">
    </search-result>
  </transition-group>
  <infinite-loading :identifier="infiniteId" @infinite="infiniteHandler">
    <LoadingSpinner slot="spinner"></LoadingSpinner>
    <EmptyCard slot="no-results"></EmptyCard>
    <div slot="no-more"></div>
    <ErrorCard
      slot="error"
      slot-scope="{ trigger }"
      :message="errorMessage"
      @retry="trigger">
    </ErrorCard>
  </infinite-loading>
</div>
</template>

<script>
import InfiniteLoading from 'vue-infinite-loading';
import SearchResult from '@/components/search/SearchResult.vue';
import ErrorCard from '@/components/search/ErrorCard.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import EmptyCard from '@/components/search/EmptyCard.vue';

export default {
  name: 'flat-search-results',
  components: {
    EmptyCard,
    LoadingSpinner,
    ErrorCard,
    SearchResult,
    InfiniteLoading,
  },
  props: {
    query: String,
    seriesFilter: Array,
    bookFilter: Array,
  },
  data() {
    return {
      results: [],
      page: 0,
      totalHits: 0,
      infiniteId: (new Date()).getTime(),
      resultDelay: 0,
      lastResult: new Date(),
      errorMessage: '',
    };
  },
  methods: {
    reset() {
      this.page = 0;
      this.results = [];
      this.infiniteId = (new Date()).getTime();
      this.resultDelay = 0;
      this.totalHits = 0;
      this.lastResult = new Date();
    },
    async search() {
      try {
        const { data: { results, totalHits } } = await this.$api.post('/books/search', {
          query: this.query,
          page: this.page,
          seriesFilter: this.seriesFilter,
          bookFilter: this.bookFilter,
        });
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
      } catch (error) {
        throw error;
      }
    },
    async infiniteHandler($state) {
      try {
        if (await this.search()) {
          this.page += 1;
          $state.loaded();
        } else {
          $state.complete();
        }
      } catch (error) {
        $state.error();
        this.errorMessage = this.$getApiError(error);
        if (this.errorMessage === null) {
          this.errorMessage = 'An unknown error has occurred, please report this!';
        }
      }
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
  },
};
</script>

<style lang="scss">
.search-result-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  position: relative;
}
</style>

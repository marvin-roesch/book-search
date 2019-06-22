<template>
<Expandable @expanded="onExpand" @closed="onClose">
  <template slot="header">
  <div class="chapter-sub-result-header">
    <span class="chapter-sub-result-title">
      {{ chapter.title }}
      <small>({{ chapter.totalOccurrences }})</small>
    </span>
    <a href="#" class="chapter-sub-result-read" @click.prevent="showContent = true">Read</a>
  </div>
  </template>
  <transition-group tag="div" class="search-result-list" name="search-slide">
    <search-result
      :result="result" :display-metadata="false"
      v-for="(result, index) in results" :key="index">
    </search-result>
  </transition-group>
  <ErrorMessage v-if="errorMessage !== null" :message="errorMessage"></ErrorMessage>
  <LoadingSpinner v-if="!resultsLoaded && errorMessage === null"></LoadingSpinner>
  <template slot="footer">
  <transition
    name="fade-slide-up"
    @after-enter="addBodyClass"
    @leave="removeBodyClass">
    <ChapterOverlay
      :id="chapter.id"
      :query="query"
      :book-title="bookTitle"
      :title="chapter.title"
      @close="showContent = false"
      v-if="showContent">
    </ChapterOverlay>
  </transition>
  </template>
</Expandable>
</template>

<script>
import axios from 'axios';
import Expandable from '@/components/Expandable.vue';
import ErrorMessage from '@/components/search/ErrorCard.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import SearchResult from '@/components/search/SearchResult.vue';
import ChapterOverlay from '@/components/search/ChapterOverlay.vue';

export default {
  name: 'ChapterSubResult',
  components: { ChapterOverlay, SearchResult, ErrorMessage, Expandable, LoadingSpinner },
  props: {
    bookTitle: String,
    chapter: Object,
    query: String,
    seriesFilter: Array,
    bookFilter: Array,
  },
  data() {
    return {
      results: [],
      resultsLoaded: false,
      cancelToken: null,
      errorMessage: null,
      showContent: false,
    };
  },
  methods: {
    async onExpand() {
      if (this.cancelToken !== null) {
        this.cancelToken.cancel();
      }
      this.cancelToken = axios.CancelToken.source();
      this.resultsLoaded = false;

      try {
        const { data: { results } } = await this.$api.post(
          `/books/chapters/${this.chapter.id}/paragraph-search`,
          {
            query: this.query,
            seriesFilter: this.seriesFilter,
            bookFilter: this.bookFilter,
          },
          {
            cancelToken: this.cancelToken.token,
          },
        );

        this.results = results.map(({ paragraphs }) => {
          const mainParagraph = paragraphs.find(p => p.main);
          return {
            mainParagraph,
            prevParagraphs: paragraphs.filter(p => p.position < mainParagraph.position),
            nextParagraphs: paragraphs.filter(p => p.position > mainParagraph.position),
            showSiblings: false,
          };
        });
      } catch (error) {
        if (!axios.isCancel(error)) {
          this.errorMessage = this.$getApiError(error);
          if (this.errorMessage === null) {
            this.errorMessage = 'An unknown error occurred, please report this!';
          }
        }
      }

      this.resultsLoaded = true;
    },
    async onClose() {
      this.results = [];
      this.errorMessage = null;
      if (this.cancelToken !== null) {
        this.cancelToken.cancel();
        this.cancelToken = null;
      }
    },
    addBodyClass() {
      document.body.classList.add('chapter-preview');
    },
    removeBodyClass() {
      document.body.classList.remove('chapter-preview');
    },
  },
};
</script>

<style lang="scss">
.chapter-sub-result {
  &-header {
    display: flex;
    align-items: center;
    pointer-events: none;
    flex-grow: 1;
    margin-right: 1rem;
  }

  &-title {
    font-size: 1.1rem;
  }

  &-read {
    pointer-events: auto;
    margin-left: auto;
  }
}
</style>

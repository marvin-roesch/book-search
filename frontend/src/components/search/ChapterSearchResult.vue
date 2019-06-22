<template>
<Expandable @expanded="onExpand" @closed="onClose">
  <template slot="header">
  {{ chapter.title }} ({{ chapter.totalOccurrences }})
  </template>
  <transition-group tag="div" class="search-result-list" name="search-slide">
    <search-result
      :result="result" :display-metadata="false"
      v-for="(result, index) in results" :key="index">
    </search-result>
  </transition-group>
  <ErrorMessage v-if="errorMessage !== null" :message="errorMessage"></ErrorMessage>
  <LoadingSpinner v-if="!resultsLoaded && errorMessage === null"></LoadingSpinner>
</Expandable>
</template>

<script>
import axios from 'axios';
import Expandable from '@/components/Expandable.vue';
import ErrorMessage from '@/components/search/ErrorCard.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import SearchResult from '@/components/search/SearchResult.vue';

export default {
  name: 'ChapterSearchResult',
  components: { SearchResult, ErrorMessage, Expandable, LoadingSpinner },
  props: {
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
          `/books/chapters/${this.chapter.id}/search`,
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
  },
};
</script>

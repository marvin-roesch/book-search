<template>
<Expandable @expanded="onExpand" @closed="onClose">
  <template slot="header">
  {{ book.title }} ({{ book.totalOccurrences }})
  </template>
  <transition-group tag="div" class="search-result-list" name="search-slide">
    <ChapterSearchResult
      :chapter="chapter"
      :query="query"
      :series-filter="seriesFilter"
      :book-filter="bookFilter"
      v-for="chapter in chapters" :key="chapter.id">
    </ChapterSearchResult>
  </transition-group>
  <ErrorMessage v-if="errorMessage !== null" :message="errorMessage"></ErrorMessage>
  <LoadingSpinner v-if="!chaptersLoaded && errorMessage === null"></LoadingSpinner>
</Expandable>
</template>

<script>
import axios from 'axios';
import Expandable from '@/components/Expandable.vue';
import ChapterSearchResult from '@/components/search/ChapterSearchResult.vue';
import ErrorMessage from '@/components/search/ErrorCard.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';

export default {
  name: 'BookSearchResult',
  components: { ErrorMessage, ChapterSearchResult, Expandable, LoadingSpinner },
  props: {
    book: Object,
    query: String,
    seriesFilter: Array,
    bookFilter: Array,
  },
  data() {
    return {
      chapters: [],
      chaptersLoaded: false,
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
      this.chaptersLoaded = false;

      try {
        const { data: { results } } = await this.$api.post(
          `/books/${this.book.id}/grouped-search`,
          {
            query: this.query,
            seriesFilter: this.seriesFilter,
            bookFilter: this.bookFilter,
          },
          {},
        );

        this.chapters = results;
      } catch (error) {
        if (!axios.isCancel(error)) {
          this.errorMessage = this.$getApiError(error);
          if (this.errorMessage === null) {
            this.errorMessage = 'An unknown error occurred, please report this!';
          }
        }
      }

      this.chaptersLoaded = true;
    },
    async onClose() {
      this.chapters = [];
      this.errorMessage = null;
      if (this.cancelToken !== null) {
        this.cancelToken.cancel();
        this.cancelToken = null;
      }
    },
  },
};
</script>

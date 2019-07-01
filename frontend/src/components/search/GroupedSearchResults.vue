<template>
<div class="search-result-container grouped-search-results">
  <transition name="search-slide">
    <h2 class="search-results-hits" v-if="totalHits > 0">Total hits: {{ totalHits }}</h2>
  </transition>
  <transition-group tag="div" class="search-result-list" name="search-slide">
    <BookSearchResult
      :book="book"
      :query="query"
      :series-filter="seriesFilter"
      :book-filter="bookFilter"
      :chapter-scope="chapterScope"
      v-for="book in books" :key="book.id">
    </BookSearchResult>
  </transition-group>
  <ErrorCard
    :message="errorMessage"
    @retry="reset"
    v-if="this.errorMessage !== null">
  </ErrorCard>
  <LoadingSpinner v-if="loading"></LoadingSpinner>
  <EmptyCard v-if="!loading && errorMessage === null && books.length === 0"></EmptyCard>
</div>
</template>

<script>
import axios from 'axios';
import ErrorCard from '@/components/search/ErrorCard.vue';
import EmptyCard from '@/components/search/EmptyCard.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import BookSearchResult from '@/components/search/BookSearchResult.vue';

export default {
  name: 'grouped-search-results',
  components: {
    BookSearchResult,
    LoadingSpinner,
    EmptyCard,
    ErrorCard,
  },
  props: {
    query: String,
    seriesFilter: Array,
    bookFilter: Array,
    chapterScope: Boolean,
  },
  data() {
    return {
      books: [],
      totalHits: 0,
      errorMessage: null,
      loading: false,
      cancelToken: null,
    };
  },
  mounted() {
    this.reset();
  },
  methods: {
    reset() {
      this.books = [];
      this.errorMessage = null;
      this.search();
    },
    async search() {
      this.loading = true;
      if (this.cancelToken !== null) {
        this.cancelToken.cancel();
      }
      this.cancelToken = axios.CancelToken.source();

      try {
        const endpoint = this.chapterScope ? 'chapter' : 'paragraph';
        const {
          data: {
            totalHits, results,
          },
        } = await this.$api.post(
          `/books/${endpoint}-search/grouped`,
          {
            query: this.query,
            seriesFilter: this.seriesFilter,
            bookFilter: this.bookFilter,
          },
          {
            cancelToken: this.cancelToken.token,
          },
        );

        this.books = results;
        this.totalHits = totalHits;
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
.grouped-search-results {
  .search-result, .chapter-end-result:first-child {
    margin-top: 0;
  }
}
</style>

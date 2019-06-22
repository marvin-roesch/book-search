<template>
<Expandable @expanded="onExpand" @closed="onClose">
  <template slot="header">
  <span class="book-search-result-title">
    {{ book.title }}
    <small>({{ book.totalOccurrences }})</small>
  </span>
  </template>
  <transition-group
    tag="div"
    class="search-result-list"
    name="search-slide"
    v-if="chapterScope">
    <ChapterSearchResult
      :result="{ book, chapter }"
      :query="query"
      v-for="chapter in chapters" :key="chapter.id">
    </ChapterSearchResult>
  </transition-group>
  <transition-group
    tag="div"
    class="search-result-list"
    name="search-slide"
    v-else>
    <ChapterSubResult
      :book-title="book.title"
      :chapter="chapter"
      :query="query"
      :series-filter="seriesFilter"
      :book-filter="bookFilter"
      v-for="chapter in chapters" :key="chapter.id">
    </ChapterSubResult>
  </transition-group>
  <ErrorMessage v-if="errorMessage !== null" :message="errorMessage"></ErrorMessage>
  <LoadingSpinner v-if="!chaptersLoaded && errorMessage === null"></LoadingSpinner>
</Expandable>
</template>

<script>
import axios from 'axios';
import Expandable from '@/components/Expandable.vue';
import ChapterSubResult from '@/components/search/ChapterSubResult.vue';
import ErrorMessage from '@/components/search/ErrorCard.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import ChapterSearchResult from '@/components/search/ChapterSearchResult.vue';

export default {
  name: 'BookSearchResult',
  components: { ChapterSearchResult, ErrorMessage, ChapterSubResult, Expandable, LoadingSpinner },
  props: {
    book: Object,
    query: String,
    seriesFilter: Array,
    bookFilter: Array,
    chapterScope: Boolean,
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
        const endpoint = this.chapterScope ? 'chapter' : 'paragraph';
        const { data: { results } } = await this.$api.post(
          `/books/${this.book.id}/${endpoint}-search/grouped`,
          {
            query: this.query,
            seriesFilter: this.seriesFilter,
            bookFilter: this.bookFilter,
          },
          {
            cancelToken: this.cancelToken.token,
          },
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

<style lang="scss">
.book-search-result {
  &-title {
    font-size: 1.25rem;
    font-weight: bold;
  }
}
</style>

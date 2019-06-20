<template>
<div class="search-result-container grouped-search-results">
  <transition name="fade-slide-up">
    <h2 v-if="totalHits > 0">Displayed hits: {{ totalHits }}</h2>
  </transition>
  <transition-group tag="div" class="search-result-list" name="fade-slide-up">
    <Expandable v-for="book in results" :key="book.id">
      <template slot="header">
      {{ book.title }}
      </template>
      <Expandable v-for="chapter in book.chapters" :key="chapter.id">
        <template slot="header">
        {{ chapter.title }} ({{ chapter.totalOccurrences }})
        </template>
        <search-result
          :result="result" :display-metadata="false"
          v-for="(result, index) in chapter.results" :key="index">
        </search-result>
      </Expandable>
    </Expandable>
  </transition-group>
  <ErrorCard
    :message="errorMessage"
    @retry="reset"
    v-if="this.errorMessage !== null">
  </ErrorCard>
  <LoadingSpinner v-if="loading"></LoadingSpinner>
  <EmptyCard v-if="!loading && errorMessage === null && results.length === 0"></EmptyCard>
</div>
</template>

<script>
import SearchResult from '@/components/search/SearchResult.vue';
import Expandable from '@/components/Expandable.vue';
import ErrorCard from '@/components/search/ErrorCard.vue';
import EmptyCard from '@/components/search/EmptyCard.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';

export default {
  name: 'grouped-search-results',
  components: {
    LoadingSpinner,
    EmptyCard,
    ErrorCard,
    Expandable,
    SearchResult,
  },
  props: {
    query: String,
    seriesFilter: Array,
    bookFilter: Array,
  },
  data() {
    return {
      results: [],
      totalHits: 0,
      errorMessage: null,
      loading: false,
    };
  },
  mounted() {
    this.reset();
  },
  methods: {
    reset() {
      this.results = [];
      this.errorMessage = null;
      this.search();
    },
    async search() {
      this.loading = true;
      try {
        const {
          data: {
            totalHits, results,
          },
        } = await this.$api.post('/books/grouped-search', {
          query: this.query,
          seriesFilter: this.seriesFilter,
          bookFilter: this.bookFilter,
        });

        this.results = results.map(b => (
          {
            ...b,
            chapters: b.chapters.map(c => (
              {
                ...c,
                results: c.results.map(({ book, chapter, paragraphs }) => {
                  const mainParagraph = paragraphs.find(p => p.main);
                  return {
                    book,
                    chapter,
                    mainParagraph,
                    prevParagraphs: paragraphs.filter(p => p.position < mainParagraph.position),
                    nextParagraphs: paragraphs.filter(p => p.position > mainParagraph.position),
                    showSiblings: false,
                  };
                }),
              }
            )),
          }
        ));
        this.totalHits = totalHits;
      } catch (error) {
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
  },
};
</script>

<style lang="scss">
.grouped-search-results {
  .search-result {
    margin-top: 0;
  }
}
</style>

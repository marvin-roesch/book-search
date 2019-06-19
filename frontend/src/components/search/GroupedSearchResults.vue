<template>
<div class="search-result-container grouped-search-results">
  <h2 v-if="totalHits > 0">Displayed hits: {{ totalHits }}</h2>
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
</div>
</template>

<script>
import SearchResult from '@/components/search/SearchResult.vue';
import Expandable from '@/components/Expandable.vue';

export default {
  name: 'grouped-search-results',
  components: {
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
      resultLookup: {},
      results: [],
      chapterPage: 0,
      totalHits: 0,
    };
  },
  mounted() {
    this.reset();
  },
  methods: {
    reset() {
      this.resultLookup = {};
      this.results = [];
      this.search();
    },
    async search() {
      try {
        const {
          data: {
            totalHits, results,
          },
        } = await this.$api.post('/grouped-search', {
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
.grouped-search-results {
  .search-result {
    margin-top: 0;
  }
}
</style>

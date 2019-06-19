<template>
<div class="search-result-container">
  <h2 v-if="totalHits > 0">Total hits: {{ totalHits }}</h2>
  <search-result :result="result" v-for="(result, index) in results" :key="`$query` + index">
  </search-result>
  <infinite-loading :identifier="infiniteId" @infinite="infiniteHandler">
  </infinite-loading>
</div>
</template>

<script>
import InfiniteLoading from 'vue-infinite-loading';
import SearchResult from '@/components/search/SearchResult.vue';

export default {
  name: 'flat-search-results',
  components: {
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
    };
  },
  methods: {
    reset() {
      this.page = 0;
      this.results = [];
      this.infiniteId = (new Date()).getTime();
    },
    async search() {
      const { data: { results, totalHits } } = await this.$api.post('/search', {
        query: this.query,
        page: this.page,
        seriesFilter: this.seriesFilter,
        bookFilter: this.bookFilter,
      });
      const mapped = results.map(({ book, chapter, paragraphs }) => {
        const mainParagraph = paragraphs.find(p => p.main);
        return {
          book,
          chapter,
          mainParagraph,
          prevParagraphs: paragraphs.filter(p => p.position < mainParagraph.position),
          nextParagraphs: paragraphs.filter(p => p.position > mainParagraph.position),
          showSiblings: false,
        };
      });

      this.results.push(
        ...mapped,
      );
      this.totalHits = totalHits;

      return mapped.length > 0;
    },
    async infiniteHandler($state) {
      if (await this.search()) {
        this.page += 1;
        $state.loaded();
      } else {
        $state.complete();
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

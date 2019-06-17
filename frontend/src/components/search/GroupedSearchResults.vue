<template>
<div class="search-result-container grouped-search-results">
  <h2 v-if="totalHits > 0">Total hits: {{ totalHits }}</h2>
  <Expandable :start-expanded="true" v-for="book in results" :key="book.id">
    <template slot="header">
    {{ book.title }}
    </template>
    <Expandable :start-expanded="true" v-for="chapter in book.chapters" :key="chapter.id">
      <template slot="header">
      {{ chapter.title }}
      </template>
      <search-result
        :result="result" :display-metadata="false"
        v-for="(result, index) in chapter.results" :key="index">
      </search-result>
    </Expandable>
  </Expandable>
  <infinite-loading :identifier="infiniteId" @infinite="infiniteHandler"></infinite-loading>
</div>
</template>

<script>
import axios from 'axios';
import InfiniteLoading from 'vue-infinite-loading';
import SearchResult from '@/components/search/SearchResult.vue';
import Expandable from '@/components/Expandable.vue';

export default {
  name: 'grouped-search-results',
  components: {
    Expandable,
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
      resultLookup: {},
      results: [],
      nextBook: null,
      nextChapter: null,
      chapterPage: 0,
      totalHits: 0,
      infiniteId: (new Date()).getTime(),
    };
  },
  methods: {
    reset() {
      this.chapterPage = 0;
      this.resultLookup = {};
      this.results = [];
      this.infiniteId = (new Date()).getTime();
    },
    async search() {
      const {
        data: {
          nextBook, nextChapter, results, totalHits,
        },
      } = await axios.post('/api/grouped-search', {
        query: this.query,
        startBook: this.nextBook,
        startChapter: this.nextChapter,
        chapterPage: this.chapterPage,
        seriesFilter: this.seriesFilter,
        bookFilter: this.bookFilter,
      });
      console.log(this.nextBook, this.nextChapter);
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

      mapped.forEach((result) => {
        const { book, chapter } = result;
        if (!this.resultLookup[book.id]) {
          const bookEntry = { id: book.id, title: book.title, chapters: [] };
          this.resultLookup[book.id] = { book: bookEntry };
          this.results.push(bookEntry);
        }
        if (!this.resultLookup[book.id][chapter.id]) {
          const chapterEntry = { chapter: chapter.id, title: chapter.title, results: [] };
          this.resultLookup[book.id][chapter.id] = chapterEntry;
          this.resultLookup[book.id].book.chapters.push(chapterEntry);
        }
        this.resultLookup[book.id][chapter.id].results.push(result);
      });
      this.totalHits = totalHits;

      return { nextBook, nextChapter, hasMoreResults: mapped.length > 0 };
    },
    async infiniteHandler($state) {
      const { nextBook, nextChapter, hasMoreResults } = await this.search();

      if (hasMoreResults) {
        this.chapterPage += 1;
        $state.loaded();
      } else if (nextBook !== null && nextChapter !== null) {
        this.nextBook = nextBook;
        this.nextChapter = nextChapter;
        this.chapterPage = 0;
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

<style lang="scss">
.grouped-search-results {
  .search-result {
    margin-top: 0;
  }
}
</style>

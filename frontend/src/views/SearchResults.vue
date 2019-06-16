<template>
<div class="search-results">
  <div class="search-results-toolbar-container">
    <div class="search-results-toolbar">
      <div class="search-results-toolbar-query">
        <div class="search-bar-icon">
          <SearchIcon></SearchIcon>
        </div>
        <input type="search" :value="$route.query.q" @change="onQueryChange">
      </div>
      <div class="search-results-toolbar-options">
        <div class="search-results-toolbar-filter">
          <span class="search-results-toolbar-filter-label">Filter:</span>
          <a href="#" @click.prevent="filterVisible = !filterVisible">
            <book-filter-summary :series="this.series"></book-filter-summary>
          </a>
          <transition name="fade">
            <div class="search-results-toolbar-filter-container" v-if="filterVisible">
              <book-filter :root="true" :series="this.series" @filtered="onFilter"></book-filter>
            </div>
          </transition>
        </div>
        <div class="search-results-toolbar-grouping">
          <label for="search-grouping">Group by</label>
          <select id="search-grouping">
            <option selected>Nothing</option>
            <option>Book</option>
            <option>Chapter</option>
          </select>
        </div>
      </div>
    </div>
  </div>
  <div class="search-result-container">
    <h2 v-if="totalHits > 0">Total hits: {{ totalHits }}</h2>
    <div :class="{ 'search-result': true, 'search-result-with-siblings': result.showSiblings }"
         @click="result.showSiblings = !result.showSiblings"
         v-for="(result, index) in results" :key="index">
      <h2>{{ result.book }} - {{ result.chapter }}</h2>
      <BidirectionalExpandable :expanded="result.showSiblings" :visible-height="24">
        <template slot="start">
        <p :class="paragraph.classes" v-html="paragraph.text"
           v-for="paragraph in result.prevParagraphs" :key="paragraph.position">
        </p>
        </template>
        <p :class="result.mainParagraph.classes" v-html="result.mainParagraph.text"></p>
        <template slot="end">
        <p :class="paragraph.classes" v-html="paragraph.text"
           v-for="paragraph in result.nextParagraphs" :key="paragraph.position">
        </p>
        </template>
      </BidirectionalExpandable>
    </div>
    <infinite-loading :identifier="infiniteId" @infinite="infiniteHandler"></infinite-loading>
  </div>
</div>
</template>

<script>
import axios from 'axios';
import BidirectionalExpandable from '@/components/BidirectionalExpandable.vue';
import { SearchIcon } from 'vue-feather-icons';
import BookFilter from '@/components/BookFilter.vue';
import BookFilterSummary from '@/components/BookFilterSummary.vue';
import InfiniteLoading from 'vue-infinite-loading';

export default {
  name: 'search-results',
  components: {
    BookFilterSummary, BookFilter, SearchIcon, BidirectionalExpandable, InfiniteLoading,
  },
  data() {
    return {
      series: [],
      results: [],
      filterVisible: false,
      page: 0,
      totalHits: 0,
      infiniteId: (new Date()).getTime(),
    };
  },
  async mounted() {
    const { data: allSeries } = await axios.get('/api/series');
    const { books, series } = this.$route.query;

    const seriesFilter = series !== undefined ? series.split('+').filter(s => s.length > 0) : null;
    const bookFilter = books !== undefined ? books.split('+').filter(s => s.length > 0) : null;

    const seriesRegex = seriesFilter === null ? null : seriesFilter.map(
      f => new RegExp(`^${f.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}($|\\\\)`),
    );

    this.series = allSeries.map(s => this.prepareSeries(s.name, s, seriesRegex, bookFilter));
  },
  methods: {
    async search() {
      const { books, series } = this.$route.query;
      const seriesFilter = series !== undefined ? series.split('+').filter(s => s.length > 0) : null;
      const bookFilter = books !== undefined ? books.split('+').filter(s => s.length > 0) : null;

      const { data: { results, totalHits } } = await axios.post('/api/search', {
        query: this.$route.query.q,
        page: this.page,
        bookFilter,
        seriesFilter,
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
    prepareSeries(path, series, seriesFilter, bookFilter) {
      return {
        ...series,
        books: series.books
          .map(b => ({
            ...b,
            selected: (seriesFilter === null && bookFilter === null)
              || bookFilter === null
              || seriesFilter.some(f => path.match(f))
              || bookFilter.includes(b.id),
          })),
        children: series.children
          .map(s => this.prepareSeries(`${path}\\${s.name}`, s, seriesFilter, bookFilter)),
      };
    },
    onQueryChange(event) {
      this.$router.push({
        name: 'search',
        query: {
          q: event.target.value,
          series: this.$route.query.series,
          books: this.$route.query.books,
        },
      });
    },
    onFilter({ series, books }) {
      this.$router.push({
        name: 'search',
        query: {
          q: this.$route.query.q,
          series: series === undefined ? undefined : series.join('+'),
          books: series === undefined ? undefined : books.join('+'),
        },
      });
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
    '$route.query': {
      handler() {
        this.page = 0;
        this.results = [];
        this.infiniteId = (new Date()).getTime();
      },
      deep: true,
    },
  },
};
</script>

<style lang="scss">
.search-results {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100vh;

  .search-results-toolbar-container {
    box-sizing: border-box;
    width: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    box-shadow: 0 0 1rem rgba(0, 0, 0, 0.1);
    position: fixed;
    background: white;
    z-index: 2000;

    .search-results-toolbar {
      padding: 1rem;
      box-sizing: border-box;
      width: 50%;
      margin: 0 auto;
      display: flex;
      align-items: stretch;
      flex-direction: column;
      @media (max-width: 960px) {
        width: 100%;
      }

      &-query {
        position: relative;
        flex-grow: 1;
        width: 100%;

        .search-bar-icon {
          display: flex;
          align-items: center;
          padding: 0 1rem;
          position: absolute;
          box-sizing: border-box;
          top: 0;
          left: 0;
          bottom: 0;
        }

        input {
          width: 100%;
          font-size: 16px;
          color: rgb(57, 63, 73);
          line-height: 1;
          font-family: inherit;
          -webkit-appearance: none;
          padding: 0.75rem 1rem 0.75rem 56px;
          border: 1px solid rgba(0, 0, 0, 0.1);
          border-radius: 4px;
          outline: 0;
          box-sizing: border-box;
        }
      }

      &-options {
        box-sizing: border-box;
        flex-grow: 1;
        margin-top: 0.5rem;
        display: flex;
        align-items: center;
      }

      &-grouping {
        margin-left: auto;
        flex-shrink: 0;

        select {
          margin-left: 0.5rem;
          font-size: 1rem;
          border: 1px solid rgba(0, 0, 0, 0.1);
          padding: 0.5rem 0.5rem;
          border-radius: 3px;
          background: none;
        }
      }

      &-filter {
        margin-right: 1rem;
        position: relative;
        display: flex;
        min-width: 0;
        flex-grow: 1;

        &-label {
          position: relative;
          z-index: 2001;
          line-height: 1rem;
        }

        a {
          position: relative;
          color: #42B983;
          text-decoration: none;
          z-index: 2001;
          flex-grow: 1;
          min-width: 0;
          margin-left: 0.25rem;
          line-height: 1rem;

          &:hover, &:active, &:focus {
            color: saturate(#42B983, 10%);
          }
        }

        &-container {
          position: absolute;
          box-sizing: border-box;
          left: -0.5rem;
          right: 0;
          top: -0.5rem;
          background: white;
          border-radius: 3px;
          padding: 2rem 0.5rem 0.5rem;
          border: 1px solid rgba(0, 0, 0, 0.1);
        }
      }
    }
  }

  .search-result-container {
    box-sizing: border-box;
    padding: 8rem 1rem 1rem;
    width: 50%;
    max-width: 960px;

    h2 {
      margin: 0.5rem 0 0;
    }

    @media (max-width: 960px) {
      width: 100%;
    }
  }

  .search-result {
    box-sizing: border-box;
    background: white;
    border-radius: 3px;
    border: 1px solid rgba(0, 0, 0, 0.05);
    box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
    padding: 1rem;
    font-family: 'Lora', serif;
    margin: 1rem 0;
    position: relative;
    cursor: pointer;

    h2 {
      position: relative;
      margin: 0;
      padding: 0;
      font-family: 'Avenir', sans-serif;
      font-size: 1.25rem;
      z-index: 1001;
    }

    p {
      margin: 0.5rem 0;

      &:first-child {
        margin-top: 0;
      }

      &:last-child {
        margin-bottom: 0;
      }
    }

    .chapterText {
      text-align: justify;
    }

    img {
      display: inline-block;
      max-width: 100%;
    }

    .centeredImage {
      text-align: center;

      img {
        max-width: 80%;
      }
    }

    .epigraphText {
      width: 60%;
      margin-left: auto;
      margin-right: auto;
      text-align: justify;
    }

    .epigraphCitation {
      width: 60%;
      margin-left: auto;
      margin-right: auto;
      text-align: right;
    }

    .italic {
      font-style: italic;
    }

    .bold {
      font-weight: bold;
    }

    .reset {
      font-style: normal;
      font-weight: normal;
    }
  }
}
</style>

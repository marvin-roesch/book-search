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
          <CheckBox :value="groupResults" @input="changeResultGrouping($event.target.checked)">
            Group by chapter
          </CheckBox>
        </div>
      </div>
    </div>
  </div>
  <grouped-search-results
    :query="$route.query.q"
    :series-filter="seriesFilter"
    :book-filter="bookFilter"
    v-if="groupResults">
  </grouped-search-results>
  <flat-search-results
    :query="$route.query.q"
    :series-filter="seriesFilter"
    :book-filter="bookFilter"
    v-else>
  </flat-search-results>
</div>
</template>

<script>
import axios from 'axios';
import { SearchIcon } from 'vue-feather-icons';
import BookFilter from '@/components/search/BookFilter.vue';
import BookFilterSummary from '@/components/search/BookFilterSummary.vue';
import FlatSearchResults from '@/components/search/FlatSearchResults.vue';
import GroupedSearchResults from '@/components/search/GroupedSearchResults.vue';
import CheckBox from '@/components/CheckBox.vue';

export default {
  name: 'search-results',
  components: {
    CheckBox,
    GroupedSearchResults,
    FlatSearchResults,
    BookFilterSummary,
    BookFilter,
    SearchIcon,
  },
  data() {
    return {
      series: [],
      filterVisible: false,
    };
  },
  computed: {
    seriesFilter() {
      const { series } = this.$route.query;
      return series !== undefined ? series.split('+').filter(s => s.length > 0) : null;
    },
    bookFilter() {
      const { books } = this.$route.query;
      return books !== undefined ? books.split('+').filter(s => s.length > 0) : null;
    },
    groupResults() {
      return this.$route.query.grouped;
    },
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
      this.$router.replace({
        name: 'search',
        query: {
          q: event.target.value,
          series: this.$route.query.series,
          books: this.$route.query.books,
          grouped: this.$route.query.grouped,
        },
      });
    },
    onFilter({ series, books }) {
      this.$router.replace({
        name: 'search',
        query: {
          q: this.$route.query.q,
          series: series === undefined ? undefined : series.join('+'),
          books: series === undefined ? undefined : books.join('+'),
          grouped: this.$route.query.grouped,
        },
      });
    },
    changeResultGrouping(grouped) {
      this.$router.replace({
        name: 'search',
        query: {
          q: this.$route.query.q,
          series: this.$route.query.series,
          books: this.$route.query.books,
          grouped: grouped || undefined,
        },
      });
    },
  },
};
</script>

<style lang="scss">
body {
  overflow-y: scroll;
}

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
          background: white;
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
    padding: 7rem 1rem 1rem;
    width: 50%;
    max-width: 960px;

    h2 {
      margin: 0.5rem 0 0;
    }

    @media (max-width: 960px) {
      width: 100%;
    }
  }
}
</style>

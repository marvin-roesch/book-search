<template>
<div class="search-results">
  <div class="search-results-toolbar-container">
    <UserPanel></UserPanel>
    <QueryPanel
      class="search-results-toolbar"
      toolbar
      :query="$route.query.q"
      :series="series"
      :chapter-scope="chapterScope"
      :group-results="groupResults"

      @ready="displayResults = true"
      @search="onSearch"
      @filter="onFilter"
      @chapter-scope="onChapterScope"
      @group-results="onGroupResults">
    </QueryPanel>
  </div>
  <grouped-search-results
    :query="$route.query.q"
    :series-filter="seriesFilter"
    :book-filter="bookFilter"
    :chapter-scope="chapterScope"
    v-if="groupResults && displayResults">
  </grouped-search-results>
  <flat-search-results
    :query="$route.query.q"
    :series-filter="seriesFilter"
    :book-filter="bookFilter"
    :chapter-scope="chapterScope"
    v-else-if="displayResults">
  </flat-search-results>
</div>
</template>

<script>
import FlatSearchResults from '@/components/search/FlatSearchResults.vue';
import GroupedSearchResults from '@/components/search/GroupedSearchResults.vue';
import QueryPanel from '@/components/search/QueryPanel.vue';
import UserPanel from '@/components/UserPanel.vue';

export default {
  name: 'search-results',
  components: {
    UserPanel,
    QueryPanel,
    GroupedSearchResults,
    FlatSearchResults,
  },
  data() {
    return {
      series: [],
      displayResults: false,
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
      const { grouped } = this.$route.query;
      return grouped === true || grouped === 'true';
    },
    chapterScope() {
      const { scope } = this.$route.query;
      return scope === 'chapters';
    },
  },
  async mounted() {
    try {
      const { data: allSeries } = await this.$api.get('/books/series');
      const { books, series } = this.$route.query;

      const seriesFilter = series !== undefined ? series.split('+').filter(s => s.length > 0) : null;
      const bookFilter = books !== undefined ? books.split('+').filter(s => s.length > 0) : null;

      const seriesRegex = seriesFilter === null ? null : seriesFilter.map(
        f => new RegExp(`^${f.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}($|\\\\)`),
      );

      this.series = allSeries.map(s => this.prepareSeries(s.name, s, seriesRegex, bookFilter));
    } catch (error) {
      this.$handleApiError(error);
    }
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
    onSearch(query) {
      this.$router.replace({
        name: 'search',
        query: {
          q: query,
          series: this.$route.query.series,
          books: this.$route.query.books,
          scope: this.$route.query.scope,
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
          books: books === undefined ? undefined : books.join('+'),
          scope: this.$route.query.scope,
          grouped: this.$route.query.grouped,
        },
      });
    },
    onChapterScope(chapterScope) {
      this.$router.replace({
        name: 'search',
        query: {
          q: this.$route.query.q,
          series: this.$route.query.series,
          books: this.$route.query.books,
          scope: chapterScope ? 'chapters' : undefined,
          grouped: this.$route.query.grouped,
        },
      });
    },
    onGroupResults(grouped) {
      this.$router.replace({
        name: 'search',
        query: {
          q: this.$route.query.q,
          series: this.$route.query.series,
          books: this.$route.query.books,
          scope: this.$route.query.scope,
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

body.chapter-preview {
  overflow: hidden !important;
}

.search-results {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100%;

  .search-results-toolbar-container {
    box-sizing: border-box;
    width: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    box-shadow: 0 0 1rem rgba(0, 0, 0, 0.1);
    position: fixed;
    background: white;
    display: flex;
    align-items: center;
    z-index: 2000;

    @media (max-width: 1200px) {
      flex-direction: column;
      align-items: flex-start;
    }

    .user-panel {
      position: absolute;
      left: 2rem;

      @media (max-width: 1200px) {
        position: relative;
        left: 0;
      }
    }

    .search-results-toolbar {
      width: 50%;

      @media (max-width: 1200px) {
        width: 70%;
        padding-top: 0;
      }

      @media (max-width: 960px) {
        width: 100%;
      }
    }
  }

  .search-result-container {
    box-sizing: border-box;
    padding: 8rem 1rem 1rem;
    width: 50%;
    max-width: 960px;
    display: flex;
    flex-direction: column;
    align-items: stretch;

    & > h2 {
      margin: 0;
    }

    @media (max-width: 1200px) {
      width: 70%;
      padding-top: 11rem;
    }

    @media (max-width: 960px) {
      width: 100%;
    }

    @media (max-width: 640px) {
      margin-top: 0.25rem;
      padding-top: 12rem;
    }
  }
}
</style>

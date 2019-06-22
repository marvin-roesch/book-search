<template>
<div class="home">
  <UserPanel></UserPanel>
  <div class="home-content">
    <h1>Book Search</h1>
    <QueryPanel
      class="search-results-toolbar"
      :query="query"
      :series="series"
      :chapter-scope="chapterScope"
      :group-results="groupResults"

      @search="onSearch"
      @filter="onFilter"
      @group-results="onGroupResults"
      @chapter-scope="onChapterScope">
    </QueryPanel>
  </div>
</div>
</template>

<script>
import QueryPanel from '@/components/search/QueryPanel.vue';
import UserPanel from '@/components/UserPanel.vue';

export default {
  name: 'home',
  components: {
    UserPanel,
    QueryPanel,
  },
  data() {
    return {
      query: '',
      series: [],
      bookFilter: undefined,
      seriesFilter: undefined,
      chapterScope: false,
      groupResults: false,
      oldQuery: {},
    };
  },
  async mounted() {
    try {
      const { data: allSeries } = await this.$api.get('/books/series');
      const { q, books, series, scope, grouped } = { ...this.oldQuery, ...this.$route.query };

      this.query = q || '';
      this.chapterScope = scope === 'chapters';
      this.groupResults = grouped === true || grouped === 'true';
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
  beforeRouteEnter(to, from, next) {
    if (from.name === 'search') {
      next((c) => {
        c.oldQuery = from.query;
      });
    } else {
      next();
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
      this.query = query;
      this.$router.push({
        name: 'search',
        query: {
          q: this.query,
          series: this.seriesFilter === undefined ? undefined : this.seriesFilter.join('+'),
          books: this.bookFilter === undefined ? undefined : this.bookFilter.join('+'),
          scope: this.chapterScope ? 'chapters' : undefined,
          grouped: this.groupResults || undefined,
        },
      });
    },
    onFilter({ series, books }) {
      this.seriesFilter = series;
      this.bookFilter = books;
    },
    onChapterScope(chapterScope) {
      this.chapterScope = chapterScope;
    },
    onGroupResults(grouped) {
      this.groupResults = grouped;
    },
  },
};
</script>

<style scoped lang="scss">
.home {
  position: relative;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;

  .user-panel {
    position: absolute;
    top: 19.5px;
    left: 32px;
  }

  &-content {
    position: relative;
    box-sizing: border-box;

    h1 {
      position: absolute;
      bottom: 100%;
      width: 100%;
      text-align: center;
    }
  }

  @media (max-width: 960px) {
    justify-content: flex-start;
    align-items: stretch;

    .user-panel {
      position: relative;
      top: 0;
      left: 0;
    }

    &-content {
      h1 {
        position: relative;
        bottom: initial;
        margin: 0;
      }
    }
  }
}
</style>

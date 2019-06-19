<template>
<div class="home">
  <UserPanel></UserPanel>
  <div class="home-content">
    <h1>Book Search</h1>
    <QueryPanel
      class="search-results-toolbar"
      :query="query"
      :series="series"
      :group-results="groupResults"

      @search="onSearch"
      @filter="onFilter"
      @group-results="onGroupResults">
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
      groupResults: false,
      oldQuery: {},
    };
  },
  async mounted() {
    const { data: allSeries } = await this.$api.get('/series');
    const { q, books, series, grouped } = { ...this.oldQuery, ...this.$route.query };

    this.query = q || '';
    this.groupResults = grouped === true || grouped === 'true';
    const seriesFilter = series !== undefined ? series.split('+').filter(s => s.length > 0) : null;
    const bookFilter = books !== undefined ? books.split('+').filter(s => s.length > 0) : null;

    const seriesRegex = seriesFilter === null ? null : seriesFilter.map(
      f => new RegExp(`^${f.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}($|\\\\)`),
    );

    this.series = allSeries.map(s => this.prepareSeries(s.name, s, seriesRegex, bookFilter));
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
          grouped: this.groupResults || undefined,
        },
      });
    },
    onFilter({ series, books }) {
      this.seriesFilter = series;
      this.bookFilter = books;
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
  height: 100vh;

  .user-panel {
    position: absolute;
    top: 1.35rem;
    right: 2rem;
  }

  &-content {
    position: relative;

    h1 {
      position: absolute;
      bottom: 100%;
      width: 100%;
      text-align: center;
    }
  }
}
</style>

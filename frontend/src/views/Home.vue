<template>
  <Fullscreen user-panel class="home">
    <div class="home-content">
      <h1>Book Search</h1>
      <QueryPanel
        auto-focus
        class="search-results-toolbar"
        :query="query"
        :series="series"
        :chapter-scope="chapterScope"
        :group-results="groupResults"

        @search="onSearch"
        @filter="onFilter"
        @group-results="onGroupResults"
        @chapter-scope="onChapterScope"
        @save-filter="saveFilter"
      >
      </QueryPanel>
    </div>
  </Fullscreen>
</template>

<script>
import { mapState } from 'vuex';
import QueryPanel from '@/components/search/QueryPanel.vue';
import Fullscreen from '@/views/Fullscreen.vue';

export default {
  name: 'home',
  components: {
    Fullscreen,
    QueryPanel,
  },
  data() {
    return {
      query: '',
      bookFilter: undefined,
      seriesFilter: undefined,
      excluded: this.$store.getters.optionalBooks,
      chapterScope: this.$store.state.auth.identity.defaultSearchScope === 'chapters',
      groupResults: this.$store.state.auth.identity.groupResultsByDefault,
      oldQuery: {},
    };
  },
  computed: mapState(['series']),
  async mounted() {
    try {
      const {
        q, books, series, excluded, scope, grouped,
      } = await new Promise((resolve) => {
        this.$nextTick(() => {
          resolve({ ...this.oldQuery, ...this.$route.query });
        });
      });

      this.query = q || '';
      if (scope !== undefined) {
        this.chapterScope = scope === 'chapters';
      }
      if (grouped !== undefined) {
        this.groupResults = grouped === true || grouped === 'true';
      }
      const seriesFilter = series !== undefined ? series.split('+').filter(s => s.length > 0) : null;
      const bookFilter = books !== undefined ? books.split('+').filter(s => s.length > 0) : null;
      const excludedFilter = excluded !== undefined
        ? excluded.split('+').filter(s => s.length > 0)
        : this.excluded;

      if (seriesFilter !== null) {
        this.seriesFilter = seriesFilter;
      }

      if (bookFilter !== null) {
        this.bookFilter = bookFilter;
      }

      this.excluded = excludedFilter;

      const seriesRegex = seriesFilter === null ? null : seriesFilter.map(
        f => new RegExp(`^${f.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}($|\\\\)`),
      );

      await this.$store.dispatch('refreshSeries');
      const fallbackFilter = {
        seriesFilter: seriesRegex,
        bookFilter,
        excluded: excluded !== undefined ? excludedFilter : this.$store.getters.optionalBooks,
      };
      if (seriesFilter !== null || bookFilter !== null || excluded !== undefined) {
        this.$store.commit('applySeriesFilter', fallbackFilter);
      } else {
        await this.$store.dispatch('loadDefaultFilter', fallbackFilter);

        if (this.$store.state.defaultFilter !== null) {
          this.bookFilter = this.$store.state.defaultFilter.bookFilter;
          this.seriesFilter = this.$store.state.defaultFilter.seriesFilter;
          this.excluded = this.$store.state.defaultFilter.excluded;
        }
      }
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
    onSearch(query) {
      this.query = query;
      this.$router.push({
        name: 'search',
        query: {
          q: this.query,
          series: !this.seriesFilter ? undefined : this.seriesFilter.join('+'),
          books: !this.bookFilter ? undefined : this.bookFilter.join('+'),
          excluded: !this.excluded ? undefined : this.excluded.join('+'),
          scope: this.chapterScope ? 'chapters' : undefined,
          grouped: this.groupResults || undefined,
        },
      });
    },
    onFilter({ series, books, excluded }) {
      this.seriesFilter = series;
      this.bookFilter = books;
      this.excluded = excluded;
    },
    onChapterScope(chapterScope) {
      this.chapterScope = chapterScope;
    },
    onGroupResults(grouped) {
      this.groupResults = grouped;
    },
    async saveFilter() {
      try {
        const { data: { message } } = await this.$api.put(
          '/auth/default-filter',
          { seriesFilter: this.seriesFilter, bookFilter: this.bookFilter, excluded: this.excluded },
        );
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
    },
  },
};
</script>

<style scoped lang="scss">
.home {
  &-content {
    position: relative;
    box-sizing: border-box;
    margin: 0 auto;

    h1 {
      position: absolute;
      bottom: 100%;
      width: 100%;
      text-align: center;
    }

    .query-panel {
      max-width: $max-content-width;
    }
  }

  @media (max-width: $max-content-width) {
    justify-content: flex-start;
    align-items: stretch;

    &-content {
      margin: 0;

      h1 {
        position: relative;
        bottom: initial;
        margin: 0;
      }
    }
  }
}
</style>

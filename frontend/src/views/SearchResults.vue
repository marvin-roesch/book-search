<template>
<div class="search-results">
  <div :class="{
    'search-results-toolbar-container': true,
    'search-results-toolbar-container-hidden': scrolledDown
  }">
    <UserPanel></UserPanel>
    <QueryPanel
      class="search-results-toolbar"
      toolbar
      :auto-focus="$route.query.focus === 'true'"
      :query="$route.query.q"
      :series="series"
      :chapter-scope="chapterScope"
      :group-results="groupResults"

      @ready="displayResults = true"
      @search="onSearch"
      @filter="onFilter"
      @chapter-scope="onChapterScope"
      @group-results="onGroupResults"
      @save-filter="saveFilter"
    >
    </QueryPanel>
  </div>
  <transition name="fade-slide-up" @after-enter="preventBodyScroll">
    <router-view></router-view>
  </transition>
  <grouped-search-results
    :query="$route.query.q"
    :series-filter="seriesFilter"
    :book-filter="bookFilter"
    :excluded="excluded"
    :chapter-scope="chapterScope"
    v-if="groupResults && displayResults">
  </grouped-search-results>
  <flat-search-results
    :query="$route.query.q"
    :series-filter="seriesFilter"
    :book-filter="bookFilter"
    :excluded="excluded"
    :chapter-scope="chapterScope"
    v-else-if="displayResults">
  </flat-search-results>
</div>
</template>

<script>
import { mapState } from 'vuex';
import FlatSearchResults from '@/components/search/FlatSearchResults.vue';
import GroupedSearchResults from '@/components/search/GroupedSearchResults.vue';
import QueryPanel from '@/components/search/QueryPanel.vue';
import UserPanel from '@/components/UserPanel.vue';
import { scrollAware } from '@/custom-directives';

export default {
  name: 'search-results',
  mixins: [scrollAware],
  components: {
    UserPanel,
    QueryPanel,
    GroupedSearchResults,
    FlatSearchResults,
  },
  data() {
    return {
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
    excluded() {
      const { excluded } = this.$route.query;
      return excluded !== undefined
        ? excluded.split('+').filter(s => s.length > 0)
        : null;
    },
    groupResults() {
      const { grouped } = this.$route.query;
      return grouped === true || grouped === 'true';
    },
    chapterScope() {
      const { scope } = this.$route.query;
      return scope === 'chapters';
    },
    ...mapState(['series']),
  },
  async mounted() {
    try {
      const { seriesFilter, bookFilter, excluded } = this;

      const seriesRegex = seriesFilter === null ? null : seriesFilter.map(
        f => new RegExp(`^${f.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}($|\\\\)`),
      );

      await this.$store.dispatch('refreshSeries');
      const fallbackFilter = {
        seriesFilter: seriesRegex,
        bookFilter,
        excluded,
      };
      if (seriesFilter !== null || bookFilter !== null || excluded !== null) {
        this.$store.commit('applySeriesFilter', fallbackFilter);
      } else {
        await this.$store.dispatch('loadDefaultFilter', fallbackFilter);
      }
    } catch (error) {
      this.$handleApiError(error);
    }
  },
  methods: {
    onSearch(query) {
      this.$router.replace({
        name: 'search',
        query: {
          ...this.$route.query,
          q: query,
        },
      });
    },
    onFilter({ series, books, excluded }) {
      this.$router.replace({
        name: 'search',
        query: {
          ...this.$route.query,
          series: series === undefined ? undefined : series.join('+'),
          books: books === undefined ? undefined : books.join('+'),
          excluded: excluded === undefined ? undefined : excluded.join('+'),
        },
      });
    },
    onChapterScope(chapterScope) {
      this.$router.replace({
        name: 'search',
        query: {
          ...this.$route.query,
          scope: chapterScope ? 'chapters' : undefined,
        },
      });
    },
    onGroupResults(grouped) {
      this.$router.replace({
        name: 'search',
        query: {
          ...this.$route.query,
          grouped: grouped || undefined,
        },
      });
    },
    preventBodyScroll() {
      document.body.classList.add('chapter-preview');
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
    background: var(--section-bg);
    display: flex;
    align-items: center;
    z-index: 2000;
    top: 0;
    transition: 0.2s top ease-out;

    @media (max-width: 1200px) {
      flex-direction: column;
      align-items: flex-start;
    }

    @media (max-width: $max-content-width) and (max-height: $max-content-width) {
      &-hidden {
        box-shadow: none;
        top: -3rem;
      }
    }

    @media (max-height: 640px) {
      &-hidden {
        box-shadow: none;
        top: -100%;
      }
    }

    .user-panel {
      position: absolute;
      top: 0.5rem;
      left: 2rem;

      @media (max-width: 1200px) {
        position: relative;
        left: 0;
        top: 0;
      }
    }

    .search-results-toolbar {
      width: 50%;
      max-width: $max-content-width;
      padding-left: 1rem;
      padding-right: 1rem;

      @media (max-width: 1200px) {
        width: 70%;
        padding-top: 0;
      }

      @media (max-width: $max-content-width) {
        width: 100%;
      }
    }
  }

  &-hits {
    color: var(--muted-text-color);
    font-size: 0.9rem;
    text-align: right;
    font-weight: normal;
  }

  .search-result-container {
    box-sizing: border-box;
    padding: 8.5rem 1rem 1rem;
    width: 50%;
    max-width: $max-content-width;
    display: flex;
    flex-direction: column;
    align-items: stretch;

    & > h2 {
      margin: 0;
    }

    @media (max-width: 1200px) {
      width: 70%;
      padding-top: 12rem;
    }

    @media (max-width: $max-content-width) {
      width: 100%;
    }

    @media (max-width: 640px) {
      margin-top: 0.25rem;
      padding-top: 12.5rem;
    }
  }
}
</style>

<template>
<shared-element
  id="query-panel"
  :class="{ 'query-panel': true, 'query-panel-toolbar': toolbar }"
  :duration="300"
  :easing="easing"
  @animation-end="$emit('ready')"
>
  <div class="query-panel-search">
    <SearchBar
      :toolbar="toolbar"
      :auto-focus="autoFocus"
      :query="query"
      @search="$emit('search', $event)"
    >
    </SearchBar>
    <QuickHelp></QuickHelp>
    <div class="query-panel-filter">
      <span class="query-panel-filter-label">Filter:</span>
      <a
        class="query-panel-filter-trigger" href="#"
        @click.prevent="filterVisible = !filterVisible" ref="filter-trigger"
      >
        <book-filter-summary :series="series" :tags="tags"></book-filter-summary>
      </a>
      <transition name="fade">
        <div
          class="query-panel-filter-container"
          v-closable="{
          exclude: ['filter-trigger'],
          handler() { filterVisible = false; }
        }"
          v-if="filterVisible"
        >
          <a
            class="query-panel-filter-action" href="#"
            @click.prevent="$refs.filter.selectAll(getSelectionFilter($event))"
          >
            All
          </a>
          &middot;
          <a
            class="query-panel-filter-action" href="#"
            @click.prevent="$refs.filter.deselectAll(getSelectionFilter($event))"
          >
            None
          </a>
          <template v-for="(books, tag) in tags">
          &middot;
          <a
            class="query-panel-filter-action" href="#"
            @click.prevent="$refs.filter.select(selectTagBooks($event, books))" :key="tag"
          >
            {{ tag }}
          </a>
          </template>
          <div class="query-panel-filter-scrollable" v-bar>
            <div>
              <book-filter
                :root="true" :series="series"
                @filtered="$emit('filter', $event)" ref="filter"
              >
              </book-filter>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
  <div class="query-panel-options">
    <div class="query-panel-scope-container">
      <span class="query-panel-scope-label">Search in</span>
      <div class="query-panel-scope">
        <input
          type="radio"
          id="query-panel-paragraphs-scope"
          value="paragraphs"
          :checked="!chapterScope"
          @input="onScopeChange"
        >
        <label for="query-panel-paragraphs-scope">Paragraphs</label>
      </div>
      <div class="query-panel-scope">
        <input
          type="radio"
          id="query-panel-chapters-scope"
          value="chapters"
          :checked="chapterScope"
          @input="onScopeChange"
        >
        <label for="query-panel-chapters-scope">Chapters</label>
      </div>
    </div>
    <div class="query-panel-grouping">
      <CheckBox
        name="group-results"
        :value="groupResults"
        @input="$emit('group-results', $event.target.checked)"
      >
        Group results
      </CheckBox>
    </div>
  </div>
</shared-element>
</template>

<script>
import { easeInOut as easing } from 'ramjet';
import { mapState } from 'vuex';
import BookFilterSummary from '@/components/search/BookFilterSummary.vue';
import BookFilter from '@/components/search/BookFilter.vue';
import CheckBox from '@/components/CheckBox.vue';
import SearchBar from '@/components/search/SearchBar.vue';
import SharedElement from '@/components/SharedElement.vue';
import QuickHelp from '@/components/search/QuickHelp.vue';

export default {
  name: 'QueryPanel',
  components: {
    QuickHelp, SharedElement, SearchBar, CheckBox, BookFilter, BookFilterSummary,
  },
  props: {
    toolbar: Boolean,
    autoFocus: Boolean,
    query: String,
    series: Array,
    chapterScope: Boolean,
    groupResults: Boolean,
  },
  data() {
    return {
      filterVisible: false,
      easing,
    };
  },
  computed: mapState(['tags']),
  async mounted() {
    try {
      await this.$store.dispatch('refreshTags');
    } catch (error) {
      this.$handleApiError(error);
    }
  },
  methods: {
    search(event) {
      this.$emit('search', event.target.value);
    },
    onScopeChange(event) {
      this.$emit('chapter-scope', event.target.value === 'chapters');
    },
    getSelectionFilter(event) {
      if (event.shiftKey) {
        return () => true;
      }

      if (event.altKey) {
        return b => b.searchedByDefault === false;
      }

      return b => b.searchedByDefault;
    },
    selectTagBooks(event, tag) {
      if (event.shiftKey) {
        return [...tag.default, ...tag.optional];
      }

      if (event.altKey) {
        return tag.optional;
      }

      return tag.default;
    },
  },
};
</script>

<style lang="scss">
.query-panel {
  box-sizing: border-box;
  margin: 0 auto;
  display: flex;
  align-items: stretch;
  padding: 1rem 0 0.5rem;
  width: 50vw;
  max-width: $max-content-width;

  &.query-panel-toolbar {
    width: auto;
  }

  @media (max-width: $max-content-width) {
    margin: 0;
    width: auto;
  }

  @media (max-width: 640px) {
    flex-direction: column;

    &.query-panel-toolbar {
      .quick-help {
        display: none;
      }
    }
  }

  &-search {
    position: relative;
    min-width: 0;
  }

  &-options {
    box-sizing: border-box;
    flex-shrink: 0;
    margin-left: 0.5rem;
    display: flex;
    flex-direction: column;
    align-items: stretch;
    position: relative;
    font-size: 0.8rem;

    @media (max-width: 640px) {
      margin-left: 0;
      margin-top: 0.5rem;
    }
  }

  &-scope {
    display: flex;
    align-items: center;

    @media (max-width: 640px) {
      margin-left: 0.25rem;
    }

    &-container {
      display: flex;
      align-items: stretch;
      flex-direction: column;

      @media (max-width: 640px) {
        flex-direction: row;
      }
    }

    input {
      margin: 0 0.125rem 0 0;
    }

    label:hover {
      cursor: pointer;
    }
  }

  &-grouping {
    margin-top: 0.25rem;

    @media (max-width: 640px) {
      margin-left: 0;
    }
  }

  &-filter {
    display: flex;
    min-width: 0;
    flex-grow: 1;
    margin-top: 0.5rem;

    &-label {
      position: relative;
      z-index: 2001;
      line-height: 1rem;
    }

    &-trigger {
      position: relative;
      display: flex;
      align-items: center;
      z-index: 2001;
      flex-grow: 1;
      min-width: 0;
      margin-left: 0.25rem;
      line-height: 1rem;
    }

    &-container {
      position: absolute;
      box-sizing: border-box;
      left: -0.5rem;
      right: 0;
      top: auto;
      margin-top: -0.5rem;
      background: var(--section-bg);
      border-radius: 3px;
      padding: 1.75rem 0.5rem 0.5rem;
      border: 1px solid rgba(0, 0, 0, 0.1);
      z-index: 2000;
      width: calc(100% + 1rem);
      max-width: $max-content-width;

      @media (max-width: $max-content-width) {
        max-width: calc(100% + 1rem);
      }
    }

    &-scrollable {
      height: 30vh;
    }

    @media (max-width: 640px) {
      margin-right: 0;
    }
  }
}

.vb > .vb-dragger {
  z-index: 5;
  width: 0.5rem;
  right: 0;
}

.vb > .vb-dragger > .vb-dragger-styler {
  backface-visibility: hidden;
  transform: rotate3d(0, 0, 0, 0);
  transition: background-color 100ms ease-out, margin 100ms ease-out, height 100ms ease-out;
  background-color: rgba($primary, .1);
  margin: 0.5rem 0;
  border-radius: 20px;
  height: calc(100% - 1rem);
  display: block;
}

.vb.vb-scrolling-phantom > .vb-dragger > .vb-dragger-styler {
  background-color: rgba($primary, .3);
}

.vb > .vb-dragger:hover > .vb-dragger-styler {
  background-color: rgba($primary, .5);
  margin: 0;
  height: 100%;
}

.vb.vb-dragging > .vb-dragger > .vb-dragger-styler {
  background-color: rgba($primary, .5);
  margin: 0;
  height: 100%;
}

.vb.vb-dragging-phantom > .vb-dragger > .vb-dragger-styler {
  background-color: rgba($primary, .5);
}
</style>

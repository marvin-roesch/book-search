<template>
<shared-element
  id="query-panel"
  :class="{ 'query-panel': true, 'query-panel-toolbar': toolbar }"
  :duration="300"
  :easing="easing"
  @animation-end="$emit('ready')">
  <SearchBar
    :toolbar="toolbar"
    :query="query"
    @search="$emit('search', $event)">
  </SearchBar>
  <div class="query-panel-quick-help">
    <ul>
      <li>Exact phrase: <code>"quote it"</code></li>
      <li>Case sensitive: <code>text.cs:Wit</code></li>
      <li>Match 0+: <code>Kala*</code></li>
      <li>Match 1: <code>Dalina?</code></li>
      <li>Boolean: <code>AND</code> (default), <code>OR</code></li>
      <li>Negation: <code>-knot</code></li>
    </ul>
  </div>
  <div class="query-panel-options">
    <div class="query-panel-filter">
      <span class="query-panel-filter-label">Filter:</span>
      <a href="#" @click.prevent="filterVisible = !filterVisible" ref="filter-trigger">
        <book-filter-summary :series="series"></book-filter-summary>
      </a>
      <transition name="fade">
        <div
          class="query-panel-filter-container"
          v-closable="{
            exclude: ['filter-trigger'],
            handler() { filterVisible = false; }
          }"
          v-if="filterVisible">
          <book-filter :root="true" :series="series" @filtered="$emit('filter', $event)">
          </book-filter>
        </div>
      </transition>
    </div>
    <div class="query-panel-grouping">
      <CheckBox
        name="chapter-scope"
        :value="chapterScope"
        @input="$emit('chapter-scope', $event.target.checked)">
        Search full chapter
      </CheckBox>
      <CheckBox
        name="group-results"
        :value="groupResults"
        @input="$emit('group-results', $event.target.checked)">
        Group by
        <template v-if="chapterScope">book</template>
        <template v-else>chapter</template>
      </CheckBox>
    </div>
  </div>
</shared-element>
</template>

<script>
import BookFilterSummary from '@/components/search/BookFilterSummary.vue';
import BookFilter from '@/components/search/BookFilter.vue';
import CheckBox from '@/components/CheckBox.vue';
import SearchBar from '@/components/search/SearchBar.vue';
import SharedElement from '@/components/SharedElement.vue';
import { easeInOut as easing } from 'ramjet';

export default {
  name: 'QueryPanel',
  components: { SharedElement, SearchBar, CheckBox, BookFilter, BookFilterSummary },
  props: {
    toolbar: Boolean,
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
  methods: {
    search(event) {
      this.$emit('search', event.target.value);
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
  flex-direction: column;
  padding: 1rem 1rem 0.5rem;

  @media (max-width: 960px) {
    margin: 0;
  }

  &-quick-help {
    box-sizing: border-box;
    flex-grow: 1;
    margin-top: 0.5rem;
    font-size: 0.8rem;
    color: rgba(0, 0, 0, 0.5);

    code {
      background: rgba(0, 0, 0, 0.05);
      padding: 0.125rem;
    }

    ul {
      list-style-type: none;
      display: flex;
      padding: 0;
      margin: 0;
      flex-wrap: wrap;

      li {
        margin-right: 0.25rem;

        &:after {
          display: inline-block;
          content: '·';
          margin-left: 0.25rem;
        }

        &:last-child {
          margin-right: 0;

          &:after {
            display: none;
          }
        }
      }
    }
  }

  @media (max-width: 640px) {
    &.query-panel-toolbar {
      .query-panel-quick-help {
        display: none;
      }
    }
  }

  &-options {
    box-sizing: border-box;
    flex-grow: 1;
    margin-top: 0.5rem;
    display: flex;
    align-items: center;

    @media (max-width: 640px) {
      flex-direction: column;
      align-items: stretch;
    }
  }

  &-grouping {
    margin-left: auto;
    flex-shrink: 0;
    display: flex;

    .checkbox {
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }

    @media (max-width: 640px) {
      margin-top: 0.25rem;
      margin-left: 0;
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
      top: -0.5rem;
      background: white;
      border-radius: 3px;
      padding: 2rem 0.5rem 0.5rem;
      border: 1px solid rgba(0, 0, 0, 0.1);
      z-index: 2000;
    }
  }
}
</style>

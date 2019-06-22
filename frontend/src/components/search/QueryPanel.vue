<template>
<shared-element
  id="query-panel"
  class="query-panel"
  :duration="300"
  :easing="easing"
  @animation-end="$emit('ready')">
  <SearchBar
    :toolbar="toolbar"
    :query="query"
    @search="$emit('search', $event)">
  </SearchBar>
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
      <CheckBox :value="groupResults" @input="$emit('group-results', $event.target.checked)">
        Group by chapter
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
  padding: 1rem;
  box-sizing: border-box;
  margin: 0 auto;
  display: flex;
  align-items: stretch;
  flex-direction: column;

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
      display: flex;
      align-items: center;
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

  @media (max-width: 960px) {
    margin: 0;
  }
}
</style>

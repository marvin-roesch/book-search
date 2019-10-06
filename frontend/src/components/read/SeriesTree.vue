<template>
<Expandable start-expanded class="series-tree">
  <template slot="header">
    <a :name="series.name" class="series-tree-anchor" v-if="includeAnchor"></a> {{ series.name }}
  </template>
  <div class="series-tree-content">
    <BookGrid :books="series.books"></BookGrid>
    <SeriesTree :series="s" v-for="s in series.children" :key="s.name"></SeriesTree>
  </div>
</Expandable>
</template>

<script>
import Expandable from '@/components/Expandable.vue';
import BookGrid from '@/components/read/BookGrid.vue';

export default {
  name: 'SeriesTree',
  components: { BookGrid, Expandable },
  props: {
    series: Object,
    includeAnchor: {
      type: Boolean,
      default: false,
    },
  },
};
</script>

<style lang="scss">
.series-tree {
  .expandable-header {
    font-weight: bold;
    font-size: 1.25rem;
  }

  &-content {
    padding-bottom: 1rem;
  }

  &-anchor {
    position: absolute;
    visibility: hidden;
    top: 0;
    left: 0;
  }
}
</style>

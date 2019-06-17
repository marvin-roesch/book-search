<template>
<span class="book-filter-summary" v-if="summary.length > 0">
  {{ summary.join(', ') }}
</span>
<span class="book-filter-summary" v-else>
  No Books
</span>
</template>

<script>
export default {
  name: 'BookFilterSummary',
  props: {
    series: Array,
  },
  data() {
    return {
      summary: [],
    };
  },
  watch: {
    series: {
      handler(newVal) {
        if (newVal.reduce((acc, s) => acc && this.allSelected(s), true)) {
          this.summary = ['All Books'];
          return;
        }

        this.summary = newVal.flatMap(s => this.summarizeSeries('', s));
      },
      deep: true,
    },
  },
  methods: {
    allSelected(series) {
      return series.books.reduce((acc, b) => acc && b.selected, true)
        && series.children.reduce((acc, s) => acc && this.allSelected(s), true);
    },
    summarizeSeries(prefix, series) {
      if (this.allSelected(series)) {
        return [`${prefix}${series.name}`];
      }

      return [
        ...series.books.filter(b => b.selected).map(b => b.title),
        ...series.children.flatMap(s => this.summarizeSeries(`${prefix}${series.name} > `, s)),
      ];
    },
  },
};
</script>

<style lang="scss">
.book-filter-summary {
  display: block;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>

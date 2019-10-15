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
    tags: Object,
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
        const selectedBooks = newVal.flatMap(s => this.getSelectedBooks(s));
        const [tagSummary, used] = this.summarizeTags(selectedBooks);
        this.summary = [...tagSummary, ...newVal.flatMap(s => this.summarizeSeries('', s, used))];
      },
      deep: true,
      immediate: true,
    },
  },
  methods: {
    allSelected(series) {
      return series.books.reduce((acc, b) => acc && b.selected, true)
        && series.children.reduce((acc, s) => acc && this.allSelected(s), true);
    },
    getSelectedBooks(series) {
      return [
        ...series.books.filter(b => b.selected).map(b => b.id),
        ...series.children.flatMap(s => this.getSelectedBooks(s)),
      ];
    },
    isSubset(subset, superset) {
      return subset.every(v => superset.includes(v));
    },
    setsMatch(a, b) {
      return this.isSubset(a, b) && this.isSubset(b, a);
    },
    summarizeTags(selectedBooks) {
      const summary = [];
      const used = [];
      Object.keys(this.tags).forEach((tag) => {
        const tagBooks = this.tags[tag];
        if (this.setsMatch(tagBooks, selectedBooks)) {
          summary.push(tag);
          used.push(...tagBooks);
        }
      });
      return [summary, used];
    },
    summarizeSeries(prefix, series, used) {
      const selectedBooks = this.getSelectedBooks(series);
      if (this.isSubset(selectedBooks, used)) {
        return [];
      }

      if (this.allSelected(series)) {
        return [`${prefix}${series.name}`];
      }

      return [
        ...series.books.filter(b => b.selected && !used.includes(b.id)).map(b => b.title),
        ...series.children.flatMap(s => this.summarizeSeries(`${prefix}${series.name} > `, s, used)),
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

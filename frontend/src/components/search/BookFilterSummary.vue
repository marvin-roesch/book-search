<template>
<span class="book-filter-summary" v-if="summary.length > 0">
  {{ summary.join(', ') }}
  <template v-if="optionalSummary.length > 0">
  plus {{ optionalSummary.join(', ') }}
  </template>
</span>
<span class="book-filter-summary" v-else>
  <template v-if="optionalSummary.length > 0">
  Only {{ optionalSummary.join(', ') }}
  </template>
  <template v-else>
  No books
  </template>
</span>
</template>

<script>
const allBooks = () => true;
const defaultBooks = b => b.searchedByDefault;
const optionalBooks = b => b.searchedByDefault === false;

export default {
  name: 'BookFilterSummary',
  props: {
    series: Array,
    tags: Object,
  },
  data() {
    return {
      summary: [],
      optionalSummary: [],
    };
  },
  watch: {
    series: {
      handler(newVal) {
        this.optionalSummary = [];
        if (newVal.reduce((acc, s) => acc && this.allSelected(s, allBooks), true)) {
          this.summary = ['All books'];
          return;
        }

        const used = [];
        const allSelectedBooks = newVal.flatMap(s => this.getSelectedBooks(s, allBooks));
        this.summary = this.summarizeTags(allSelectedBooks, used, 'all');

        const defaultSelectedBooks = newVal.flatMap(s => this.getSelectedBooks(s, defaultBooks));
        const allDefaultSelected = newVal.reduce(
          (acc, s) => acc && this.allSelected(s, defaultBooks),
          true,
        );
        if (this.summary.length === 0 && allDefaultSelected) {
          this.summary.push('All default books');
          used.push(...defaultSelectedBooks);
        }

        this.summary.push(
          ...this.summarizeTags(defaultSelectedBooks, used, 'default'),
          ...newVal.flatMap(s => this.summarizeSeries('', s, '', false, used, allBooks)),
          ...newVal.flatMap(s => this.summarizeSeries('', s, ' default', true, used, defaultBooks)),
        );

        const optionalSelectedBooks = newVal.flatMap(s => this.getSelectedBooks(s, optionalBooks));
        const allOptionalSelected = newVal.reduce(
          (acc, s) => acc && this.allSelected(s, optionalBooks),
          true,
        );
        if (
          allOptionalSelected
          && !optionalSelectedBooks.some(b => used.includes(b))
          && optionalSelectedBooks.length > 0
        ) {
          this.optionalSummary.push('all optional books');
          return;
        }

        this.optionalSummary.push(
          ...this.summarizeTags(optionalSelectedBooks, used, 'optional'),
          ...newVal.flatMap(s => this.summarizeSeries(
            '',
            s,
            ' optional',
            true,
            used,
            optionalBooks,
          )),
        );
      },
      deep: true,
      immediate: true,
    },
  },
  methods: {
    allSelected(series, filter) {
      return series.books.filter(filter)
        .reduce(
          (acc, b) => acc && b.selected,
          true,
        ) && series.children.reduce((acc, s) => acc && this.allSelected(s, filter), true);
    },
    getSelectedBooks(series, filter) {
      return [
        ...series.books
          .filter(b => b.selected && filter(b))
          .map(b => b.id),
        ...series.children.flatMap(s => this.getSelectedBooks(s, filter)),
      ];
    },
    isSubset(subset, superset) {
      return subset.every(v => superset.includes(v));
    },
    setsMatch(a, b) {
      return this.isSubset(a, b) && this.isSubset(b, a);
    },
    summarizeTags(selectedBooks, used, mode) {
      const summary = [];
      Object.keys(this.tags).forEach((tag) => {
        const tagBooks = mode === 'all'
          ? [...this.tags[tag].default, ...this.tags[tag].optional]
          : this.tags[tag][mode];

        if (tagBooks.length === 0) {
          return;
        }

        if (tagBooks.some(b => used.includes(b))) {
          return;
        }

        if (this.setsMatch(tagBooks, selectedBooks)) {
          summary.push(mode === 'all' ? tag : `${tag} ${mode}`);
          used.push(...tagBooks);
        }
      });
      return summary;
    },
    summarizeSeries(prefix, series, suffix, individualBooks, used, filter) {
      const selectedBooks = this.getSelectedBooks(series, filter);
      if (this.isSubset(selectedBooks, used)) {
        return [];
      }

      if (this.allSelected(series, filter) && selectedBooks.length > 1) {
        used.push(...selectedBooks);
        return [`${prefix}${series.name}${suffix}`];
      }

      const includedBooks = individualBooks
        ? series.books.filter(b => b.selected && filter(b) && !used.includes(b.id))
        : [];

      used.push(...includedBooks.map(b => b.id));

      return [
        ...includedBooks.map(b => b.title),
        ...series.children.flatMap(s => this.summarizeSeries(
          `${prefix}${series.name} > `,
          s,
          suffix,
          individualBooks,
          used,
          filter,
        )),
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

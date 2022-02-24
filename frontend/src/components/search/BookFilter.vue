<template>
<ul class="book-filter">
  <li class="book-filter-series" v-for="s in includedSeries" :key="s.name">
    <CheckBox
      :name="s.name"
      :value="allSelected(s)"
      :class="{ 'book-filter__series--optional': allNonDefault(s) }"
      @input="toggleSeries(s, $event.target.checked)"
    >
      {{ s.name }}
    </CheckBox>
    <ul class="book-filter-books">
      <li v-for="book in searchBooks(s.books)" :key="book.id">
        <CheckBox
          :name="book.id"
          :value="book.selected"
          @input="toggleBook(book, $event.target.checked)"
          :class="{ 'book-filter__book--optional': book.searchedByDefault === false }"
        >
          {{ book.title }}
        </CheckBox>
      </li>
    </ul>
    <BookFilter
      class="book-filter-child"
      :root="false"
      :series="s.children"
      :search="search"
      @filtered="onChildFiltered"
    >
    </BookFilter>
  </li>
  <li v-if="root && includedSeries.length === 0">
    No books or series match your search.
  </li>
</ul>
</template>

<script>
import CheckBox from '@/components/CheckBox.vue';

export default {
  name: 'BookFilter',
  components: { CheckBox },
  props: {
    root: Boolean,
    series: Array,
    search: String,
  },
  computed: {
    includedSeries() {
      return this.series.filter(this.shouldIncludeSeries);
    },
  },
  methods: {
    selectAll(filter) {
      this.series.forEach(s => this.toggleSeriesImpl(s, true, filter));
      if (this.root) {
        this.$emit('filtered', this.buildFilter());
      }
    },
    deselectAll(filter) {
      this.series.forEach(s => this.toggleSeriesImpl(s, false, filter));
      if (this.root) {
        this.$emit('filtered', this.buildFilter());
      }
    },
    select(books) {
      this.series.forEach(s => this.toggleBooksImpl(s, books));
      if (this.root) {
        this.$emit('filtered', this.buildFilter());
      }
    },
    allNonDefault(series) {
      return series.books.every(b => b.searchedByDefault === false)
        && series.children.every(this.allNonDefault);
    },
    allSelected(series) {
      if (this.allNonDefault(series)) {
        return series.books.every(b => b.selected) && series.children.every(this.allSelected);
      }

      const defaultBooks = series.books.filter(b => b.searchedByDefault);

      return defaultBooks.every(b => b.selected)
        && series.children.every(this.allSelected);
    },
    toggleSeries(series, value) {
      this.toggleSeriesImpl(series, value, () => true);
      if (this.root) {
        this.$emit('filtered', this.buildFilter());
      } else {
        this.$emit('filtered');
      }
    },
    toggleSeriesImpl(series, value, filter) {
      series.books.filter(filter).forEach((b) => {
        b.selected = value;
      });
      series.children.forEach((s) => {
        this.toggleSeriesImpl(s, value, filter);
      });
    },
    toggleBooksImpl(series, books) {
      series.books.forEach((b) => {
        b.selected = books.includes(b.id);
      });
      series.children.forEach((s) => {
        this.toggleBooksImpl(s, books);
      });
    },
    toggleBook(book, selected) {
      book.selected = selected;
      if (this.root) {
        this.$emit('filtered', this.buildFilter());
      } else {
        this.$emit('filtered');
      }
    },
    buildExclusions(series) {
      const exclusions = [];

      exclusions.push(
        ...series.books
          .filter(b => b.searchedByDefault !== true && !b.selected)
          .map(b => b.id),
      );

      series.children.forEach(this.buildExclusions);

      return exclusions;
    },
    buildFilter() {
      if (this.series.every(this.allSelected)) {
        const excluded = this.series.reduce(
          (acc, s) => [...acc, ...(this.buildExclusions(s) || [])],
          [],
        );

        return {
          series: undefined,
          books: undefined,
          excluded: excluded.length > 0 ? excluded : undefined,
        };
      }

      return this.series.reduce(
        ({ series, books, excluded }, c) => {
          const {
            series: cSeries,
            books: cBooks,
            excluded: cExcluded,
          } = this.buildSeriesFilter('', c);

          return ({
            series: [...series, ...cSeries],
            books: [...books, ...cBooks],
            excluded: [...excluded, ...cExcluded],
          });
        },
        { series: [], books: [], excluded: [] },
      );
    },
    buildSeriesFilter(seriesPath, s) {
      if (this.allSelected(s)) {
        return { series: [`${seriesPath}${s.name}`], books: [], excluded: this.buildExclusions(s) };
      }

      const currentBooks = s.books.filter(b => b.selected).map(b => b.id);

      const { series, books, excluded } = s.children.reduce(
        ({ series: accSeries, books: accBooks, excluded: accExcluded }, c) => {
          const {
            series: cSeries,
            books: cBooks,
            excluded: cExcluded,
          } = this.buildSeriesFilter(
            `${seriesPath}${s.name}\\`,
            c,
          );
          return ({
            series: [...accSeries, ...cSeries],
            books: [...accBooks, ...cBooks],
            excluded: [...accExcluded, ...cExcluded],
          });
        },
        { series: [], books: [], excluded: [] },
      );

      return {
        series,
        books: [...currentBooks, ...books],
        excluded,
      };
    },
    onChildFiltered() {
      if (this.root) {
        this.$emit('filtered', this.buildFilter());
      } else {
        this.$emit('filtered');
      }
    },
    shouldIncludeSeries(series) {
      return series.name.includes(this.search)
        || (series.books.length > 0 && series.books.some(book => book.title.includes(this.search)))
        || (series.children.length > 0 && series.children.some(this.shouldIncludeSeries));
    },
    searchBooks(books) {
      return books.filter(book => book.title.includes(this.search));
    },
  },
};
</script>

<style lang="scss">
.book-filter {
  box-sizing: border-box;
  list-style-type: none;
  display: flex;
  flex-wrap: wrap;
  padding: 0;
  margin: 0;
  flex-direction: column;

  &-series {
    margin-right: 1rem;
  }

  &-books {
    list-style-type: none;
    margin: 0;
    padding: 0 0 0 1rem;
  }

  li {
    padding: 0.125rem 0;
  }

  &-child {
    padding-left: 1rem;

    .book-filter-series {
      margin: 0;
    }
  }

  &__series--optional, &__book--optional {
    .checkbox-label {
      opacity: 0.5 !important;
    }
  }
}
</style>

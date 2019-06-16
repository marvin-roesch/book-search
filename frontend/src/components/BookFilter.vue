<template>
<ul class="book-filter">
  <li class="book-filter-series" v-for="s in series" :key="s.name">
    <CheckBox
      :name="s.name"
      :value="allSelected(s)"
      @input="toggleSeries(s, $event.target.checked)"
    >
      {{ s.name }}
    </CheckBox>
    <ul class="book-filter-books">
      <li v-for="book in s.books" :key="book.id">
        <CheckBox
          :name="book.id"
          :value="book.selected"
          @input="toggleBook(book, $event.target.checked)"
        >
          {{ book.title }}
        </CheckBox>
      </li>
    </ul>
    <BookFilter class="book-filter-child" :series="s.children"></BookFilter>
  </li>
</ul>
</template>

<script>
import CheckBox from '@/components/CheckBox.vue';

export default {
  name: 'BookFilter',
  components: { CheckBox },
  props: {
    series: Array,
  },
  methods: {
    allSelected(series) {
      return series.books.reduce((acc, b) => acc && b.selected, true)
        && series.children.reduce((acc, s) => acc && this.allSelected(s), true);
    },
    toggleSeries(series, value) {
      this.toggleSeriesImpl(series, value);
      this.$emit('filtered', this.buildFilter());
    },
    toggleSeriesImpl(series, value) {
      series.books.forEach((b) => {
        b.selected = value;
      });
      series.children.forEach((s) => {
        this.toggleSeriesImpl(s, value);
      });
    },
    toggleBook(book, selected) {
      book.selected = selected;
      this.$emit('filtered', this.buildFilter());
    },
    buildFilter() {
      return this.series.reduce(
        ({ series, books }, c) => {
          const { series: cSeries, books: cBooks } = this.buildSeriesFilter('', c);
          return ({
            series: [...series, ...cSeries],
            books: [...books, ...cBooks],
          });
        },
        { series: [], books: [] },
      );
    },
    buildSeriesFilter(seriesPrefix, s) {
      const seriesPath = seriesPrefix.length === 0 ? s.name : `${seriesPrefix}\\${s.name}`;

      if (this.allSelected(s)) {
        return { series: [seriesPath], books: [] };
      }

      const currentBooks = s.books.filter(b => b.selected).map(b => b.id);

      const { series, books } = s.children.reduce(
        ({ accSeries, accBooks }, c) => {
          const { series: cSeries, books: cBooks } = this.buildSeriesFilter(seriesPath, c);
          return ({
            series: [...accSeries, ...cSeries],
            books: [...accBooks, cBooks],
          });
        },
        { series: [], books: [] },
      );

      return {
        series,
        books: [...currentBooks, ...books],
      };
    },
  },
};
</script>

<style scoped lang="scss">
.book-filter {
  box-sizing: border-box;
  list-style-type: none;
  display: flex;
  padding: 0;
  margin: 0;

  &-series {
    margin-right: 1rem;
  }

  &-books {
    list-style-type: none;
    margin: 0;
    padding: 0 0 0 2rem;
  }

  &-child {
    flex-direction: column;
    padding-left: 2rem;

    .book-filter-series {
      margin: 0;
    }
  }
}
</style>

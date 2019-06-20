<template>
<Card class="book-list">
  <template slot="title">
  Books
  <Button slim :to="{name: 'book-upload'}">Upload</Button>
  </template>
  <SeriesEntry :series="series" class="book-list-root" @book-deleted="refresh"></SeriesEntry>
</Card>
</template>

<script>
import Card from '@/components/Card.vue';
import SeriesEntry from '@/components/management/SeriesEntry.vue';
import Button from '@/components/Button.vue';

export default {
  name: 'BookList',
  components: { Button, SeriesEntry, Card },
  data() {
    return {
      series: [],
    };
  },
  async mounted() {
    await this.refresh();
  },
  methods: {
    async refresh() {
      try {
        const { data: series } = await this.$api.get('/books/series');
        this.series = series;
      } catch (error) {
      }
    },
  },
};
</script>

<style scoped lang="scss">
.book-list {
  margin: 0 auto;
  width: 30vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  align-items: stretch;

  &-root {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }
}
</style>

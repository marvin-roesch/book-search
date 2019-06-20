<template>
<Card class="book-list">
  <template slot="title">
  Books
  <Button slim :to="{name: 'book-upload'}" :disabled="reindexing">Upload</Button>
  <Button slim @click="reindexAll" :loading="reindexing" :disabled="reindexing">Reindex all</Button>
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
      reindexing: false,
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
        this.$handleApiError(error);
      }
    },
    async reindexAll() {
      this.reindexing = true;
      try {
        const { data: { message } } = await this.$api.post('/books/reindex-all');
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
      await this.refresh();
      this.reindexing = false;
    },
  },
};
</script>

<style lang="scss">
.book-list {
  margin: 0 auto;
  width: 30vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  align-items: stretch;

  h2 {
    display: flex;
    align-items: center;

    .button {
      display: block;
      margin-left: 0.5rem;
    }
  }

  &-root {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }
}
</style>
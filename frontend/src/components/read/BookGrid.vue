<template>
<div class="book-grid">
  <BookThumbnail :book="book" v-for="book in books" :key="book.id"></BookThumbnail>
</div>
</template>

<script>
import BookThumbnail from '@/components/read/BookThumbnail.vue';

export default {
  name: 'BookGrid',
  components: { BookThumbnail },
  props: {
    books: Array,
  },
};
</script>

<style lang="scss">
.book-grid {
  display: flex;
  flex-wrap: wrap;

  & > * {
    margin-right: 1rem;
    margin-bottom: 1rem;
  }
}
</style>

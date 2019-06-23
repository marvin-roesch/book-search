<template>
<div class="book-thumbnail">
  <img :src="`/api/books/${book.id}/cover`" :alt="`'${book.title}' Cover`">
</div>
</template>

<script>
export default {
  name: 'BookThumbnail',
  props: {
    book: Object,
  },
};
</script>

<style lang="scss">
.book-thumbnail {
  max-height: 19rem;

  img {
    max-height: 19rem;
  }
}
</style>

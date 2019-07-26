<template>
<Fullscreen user-panel="">
  <Card class="new-book" title="Upload new book">
    <BookUploader></BookUploader>
  </Card>
</Fullscreen>
</template>

<script>
import BookUploader from '@/components/management/wizard/BookUploader.vue';
import Fullscreen from '@/views/Fullscreen.vue';
import Card from '@/components/Card.vue';

export default {
  name: 'new-book',
  components: { Card, Fullscreen, BookUploader },
};
</script>

<style lang="scss">
.new-book {
  margin: 0 auto;
  text-align: center;
}
</style>

<template>
<Fullscreen user-panel="">
  <BookUploader></BookUploader>
</Fullscreen>
</template>

<script>
import BookUploader from '@/components/management/wizard/BookUploader.vue';
import Fullscreen from '@/views/Fullscreen.vue';

export default {
  name: 'new-book',
  components: { Fullscreen, BookUploader },
};
</script>

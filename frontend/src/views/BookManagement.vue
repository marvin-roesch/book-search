<template>
<Fullscreen user-panel>
  <BookList></BookList>
</Fullscreen>
</template>

<script>
import BookList from '@/components/management/BookList.vue';
import Fullscreen from '@/views/Fullscreen.vue';

export default {
  name: 'book-management',
  components: { Fullscreen, BookList },
};
</script>

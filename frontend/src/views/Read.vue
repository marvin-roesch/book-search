<template>
<Fullscreen user-panel>
  <BookGrid></BookGrid>
</Fullscreen>
</template>

<script>
import Fullscreen from '@/views/Fullscreen.vue';
import BookGrid from '@/components/read/BookGrid.vue';

export default {
  name: 'read',
  components: { BookGrid, Fullscreen },
};
</script>

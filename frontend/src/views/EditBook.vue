<template>
<Fullscreen user-panel>
  <BookWizard></BookWizard>
</Fullscreen>
</template>

<script>
import BookWizard from '@/components/management/wizard/BookWizard.vue';
import Fullscreen from '@/views/Fullscreen.vue';

export default {
  name: 'edit-book',
  components: { Fullscreen, BookWizard },
};
</script>

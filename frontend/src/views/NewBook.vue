<template>
<div class="new-book">
  <BookWizard></BookWizard>
</div>
</template>

<script>
import BookWizard from '@/components/management/wizard/BookWizard.vue';

export default {
  name: 'new-book',
  components: { BookWizard },
};
</script>

<style lang="scss">
.new-book {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
}
</style>

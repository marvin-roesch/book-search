<template>
<div class="reupload-book">
  <h2>Upload new book version</h2>
  <BookUploader :book-id="id"></BookUploader>
</div>
</template>

<script>
import BookUploader from '@/components/management/wizard/BookUploader.vue';

export default {
  name: 'reupload-book',
  components: { BookUploader },
  data() {
    return {
      id: null,
    };
  },
  mounted() {
    const { id } = this.$route.params;
    if (!id) {
      this.$router.replace({ name: 'book-management' });
      return;
    }
    this.id = id;
  },
};
</script>

<style lang="scss">
.reupload-book {
  text-align: center;
}
</style>

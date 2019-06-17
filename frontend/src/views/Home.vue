<template>
<div class="home">
  <div class="home-content">
    <h1>Book Search</h1>
    <SearchBar></SearchBar>
    <div class="book-list">
      <router-link :to="{name: 'book-upload'}">Add Book</router-link>
    </div>
  </div>
</div>
</template>

<script>
import SearchBar from '@/components/search/SearchBar.vue';

export default {
  name: 'home',
  components: {
    SearchBar,
  },
};
</script>

<style scoped lang="scss">
.home {
  &-content {
    position: relative;

    h1 {
      position: absolute;
      bottom: 100%;
      width: 100%;
      text-align: center;
    }
  }
}

.book-list {
  position: absolute;
  top: 100%;
  width: 100%;
  padding-top: 1rem;
  text-align: center;
}
</style>

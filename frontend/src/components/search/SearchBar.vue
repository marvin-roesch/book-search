<template>
<div :class="toolbar ? 'toolbar-search-bar' : 'search-bar'">
  <div class="search-bar-icon">
    <SearchIcon></SearchIcon>
  </div>
  <input type="search" :value="query" @keydown.enter="search" ref="search">
</div>
</template>

<script>
import { SearchIcon } from 'vue-feather-icons';

export default {
  name: 'SearchBar',
  components: { SearchIcon },
  props: {
    query: String,
    toolbar: Boolean,
  },
  mounted() {
    if (!this.toolbar) {
      this.$refs.search.focus();
    }
  },
  methods: {
    search(event) {
      this.$emit('search', event.target.value);
    },
  },
};
</script>

<style lang="scss">
.search-bar {
  position: relative;
  width: 50vw;
  z-index: 2500;

  @media (max-width: 960px) {
    width: 100%;
  }

  &-icon {
    display: flex;
    align-items: center;
    padding: 0 1rem;
    position: absolute;
    box-sizing: border-box;
    top: 0;
    left: 0;
    bottom: 0;
  }

  input {
    width: 100%;
    font-size: 16px;
    color: rgb(57, 63, 73);
    line-height: 1;
    box-shadow: rgba(0, 0, 0, 0.1) 0 0 0 1px, rgba(0, 0, 0, 0.1) 0 2px 4px;
    font-family: inherit;
    -webkit-appearance: none;
    padding: 1.5rem 16px 1.5rem 56px;
    border-width: 0;
    border-style: initial;
    border-color: initial;
    border-image: initial;
    border-radius: 4px;
    outline: 0;
    box-sizing: border-box;
  }
}

.toolbar-search-bar {
  position: relative;
  flex-grow: 1;
  width: 100%;
  z-index: 2500;

  .search-bar-icon {
    display: flex;
    align-items: center;
    padding: 0 1rem;
    position: absolute;
    box-sizing: border-box;
    top: 0;
    left: 0;
    bottom: 0;
  }

  input {
    width: 100%;
    font-size: 16px;
    color: rgb(57, 63, 73);
    line-height: 1;
    font-family: inherit;
    -webkit-appearance: none;
    padding: 0.75rem 1rem 0.75rem 56px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 4px;
    outline: 0;
    box-sizing: border-box;
    background: white;
  }
}
</style>

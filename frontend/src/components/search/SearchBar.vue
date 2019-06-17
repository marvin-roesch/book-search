<template>
<div class="search-bar">
  <div class="search-bar-icon">
    <SearchIcon></SearchIcon>
  </div>
  <input type="text">
</div>
</template>

<script>
import { SearchIcon } from 'vue-feather-icons';

export default {
  name: 'SearchBar',
  components: { SearchIcon },
  props: {},
};
</script>

<style scoped lang="scss">
.search-bar {
  position: relative;
  width: 50vw;

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
</style>

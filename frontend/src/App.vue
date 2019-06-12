<template>
<div id="app">
  <router-view/>
</div>
</template>

<script>
import Button from '@/components/Button.vue';

export default {
  components: { Button },
};
</script>


<style lang="scss">
body {
  margin: 0;
  padding: 0;
  background: linear-gradient(-9deg, rgba(61, 125, 186, 0.5), rgba(51, 97, 148, 0.5));
  width: 100vw;
  height: 100vh;
}

#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100vw;
  height: 100vh;
}
</style>

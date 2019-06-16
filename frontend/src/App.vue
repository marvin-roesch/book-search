<template>
<div id="app">
  <router-view/>
</div>
</template>

<style lang="scss">
body {
  margin: 0;
  padding: 0;
  //background: linear-gradient(-9deg, rgba(61, 125, 186, 0.5), rgba(51, 97, 148, 0.5));
  width: 100%;
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
  width: 100%;
  height: 100vh;
}
</style>

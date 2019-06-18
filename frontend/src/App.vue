<template>
<div id="app">
  <transition name="fade">
    <router-view/>
  </transition>
</div>
</template>

<style lang="scss">
body {
  margin: 0;
  padding: 0;
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

.fade-enter-active, .fade-leave-active {
  position: absolute;
  transition: opacity .3s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}
</style>

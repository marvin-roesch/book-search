import Vue from 'vue';
import Vuex from 'vuex';
import auth from '@/store/auth';
import notifications from '@/store/notifications';

Vue.use(Vuex);

const initialState = {
  darkMode: window.localStorage.getItem('darkMode') === 'true',
};

const mutations = {
  setDarkMode(state, enabled) {
    state.darkMode = enabled;
    window.localStorage.setItem('darkMode', enabled ? 'true' : 'false');
  },
};

export default new Vuex.Store({
  state: initialState,
  mutations,
  modules: {
    auth,
    notifications,
  },
});

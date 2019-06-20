import Vue from 'vue';
import Vuex from 'vuex';
import axios from 'axios';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    identity: null,
  },
  getters: {
    authorized(state) {
      return state.identity !== null;
    },
  },
  mutations: {
    setIdentity(state, identity) {
      state.identity = identity;
    },
    resetIdentity(state) {
      state.identity = null;
    },
  },
  actions: {
    async checkIdentity({ commit }) {
      try {
        const { data: identity } = await axios.get('/api/auth/identity');
        commit('setIdentity', identity);
        return true;
      } catch (error) {
        if (error.response && error.response.status === 401) {
          commit('setIdentity', null);
        }
      }
      return false;
    },
    async login({ commit }, payload) {
      try {
        const { data: { firstLogin, user: identity } } = await axios.post('/api/auth/login', payload);
        commit('setIdentity', identity);
        return firstLogin;
      } catch (error) {
        if (error.response && error.response.status === 401) {
          commit('setIdentity', null);
          throw error;
        }
      }
      return false;
    },
    async logout({ commit }) {
      try {
        const { data: { message } } = await axios.post('/api/auth/logout');
        commit('setIdentity', null);
      } catch (error) {
      }
    },
  },
});

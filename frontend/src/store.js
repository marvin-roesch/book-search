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
  },
  actions: {
    async checkIdentity({ commit }) {
      try {
        const { data: identity } = await axios.get('/api/identity');
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
        const { data: identity } = await axios.post('/api/login', payload);
        commit('setIdentity', identity);
      } catch (error) {
        if (error.response && error.response.status === 401) {
          commit('setIdentity', null);
          throw error;
        }
      }
    },
  },
});

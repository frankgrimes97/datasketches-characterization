/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#include "hll_cross_language_profile.hpp"

#include <string>
#include <fstream>

#include <hll.hpp>

namespace datasketches {

void hll_cross_language_profile::run() {
  /*
  for (int t = 0; t < 3; t++) {
    std::string t_str = std::to_string(t * 2 + 4);
    for (int lg_k = BASE_LG_K; lg_k <= BASE_LG_K + 1; lg_k++) {
      for (int m = 0; m < 4; m++) {
        std::string mode = MODE_ARR[m];
        std::string fname = "Hll" + t_str + "K" + std::to_string(lg_k) + mode + ".bin";
        std::cout << "reading " << fname << std::endl;
        std::ifstream is;
        is.exceptions(std::ios::failbit | std::ios::badbit);
        is.open(DATA_PATH + "/" + fname, std::ios::binary);
        hll_sketch sketch = hll_sketch::deserialize(is);
      }
    }
  }
  */
  for (int i = 0; i < 4; i++) {
    const int lg_k_seq = LG_K_SEQ_ARR[i];
    const int u_lg_k = BASE_LG_K + ((lg_k_seq & 4) > 0 ? 1 : 0);
    const int gdt_lg_k = BASE_LG_K + ((lg_k_seq & 2) > 0 ? 1 : 0);
    const int src_lg_k = BASE_LG_K + ((lg_k_seq & 1) > 0 ? 1 : 0);
    for (int gdt_m = 0; gdt_m < 4; gdt_m++) {
      for (int src_m = 0; src_m < 4; src_m++) {
        for (int t = 0; t < 3; t++) {
          std::string t_str = std::to_string(t * 2 + 4);
          std::string gdt_fname = "Hll8K" + std::to_string(gdt_lg_k) + MODE_ARR[gdt_m];
          std::string src_fname = "Hll" + t_str + "K" + std::to_string(src_lg_k) + MODE_ARR[src_m];
          std::string u_fname = "UK" + std::to_string(u_lg_k) + "_" + gdt_fname + "_" + src_fname;

          std::cout << "reading gadget " << gdt_fname << std::endl;
          std::ifstream gdt_is;
          gdt_is.exceptions(std::ios::failbit | std::ios::badbit);
          gdt_is.open(DATA_PATH + "/" + gdt_fname + ".bin", std::ios::binary);
          hll_sketch gdt = hll_sketch::deserialize(gdt_is);

          std::cout << "reading source " << src_fname << std::endl;
          std::ifstream src_is;
          src_is.exceptions(std::ios::failbit | std::ios::badbit);
          src_is.open(DATA_PATH + "/" + src_fname + ".bin", std::ios::binary);
          hll_sketch src = hll_sketch::deserialize(src_is);

          std::cout << "reading union " << u_fname << std::endl;
          std::ifstream u_is;
          u_is.exceptions(std::ios::failbit | std::ios::badbit);
          u_is.open(DATA_PATH + "/" + u_fname + ".bin", std::ios::binary);
//          hll_union ref_union = hll_union::deserialize(u_is);
//          u_is.seekg(0);
          hll_union::vector_bytes expected_bytes;
          auto begin = std::istreambuf_iterator<char>(u_is);
          auto end = std::istreambuf_iterator<char>();
          for (auto it = begin; it != end; ++it) expected_bytes.push_back(*it);
          std::cout << "Expected size " << expected_bytes.size() << std::endl;

          hll_union test_union(u_lg_k);
          test_union.update(gdt);
          test_union.update(src);
          hll_union::vector_bytes actual_bytes = test_union.serialize_updatable();
          std::cout << "Actual size " << actual_bytes.size() << std::endl;
          if (actual_bytes != expected_bytes) {
            std::cerr << "Actual bytes:" << std::endl << std::hex;
            for (auto byte: actual_bytes) std::cerr << std::setw(2) << std::setfill('0') << (int) byte;
            std::cerr  << std::endl;
            std::cerr << "Expected bytes:" << std::endl << std::hex;
            for (auto byte: expected_bytes) std::cerr << std::setw(2) << std::setfill('0') << (int) byte;
            std::cerr  << std::endl;
            throw std::runtime_error(u_fname + " mismatch");
          }
        }
      }
    }
  }
}

void hll_cross_language_profile::build_file_map_and_file_list() {
}

}

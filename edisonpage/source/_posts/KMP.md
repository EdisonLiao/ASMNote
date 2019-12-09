---
title: KMP算法
---

最近老在想什么是程序猿的软实力，思来想去，发觉是解决问题的能力、学习能力、英语阅读能力、算法......要仔细罗列的话，大概还有很多很多。
就在这思绪的停顿片刻，余光瞥见书桌上摆放的《算法 第四版》，书是年初买的，每次看都是草草翻阅，实在是惭愧，于是乎奔着提升软实力的目标（借口），决定按章看完，然后上LeetCode按Tags去刷题，啊...真的是，人一旦有了小目标，心里踏实很多（三分钟热血？？）

<!--more-->

### 什么是KMP
KMP算法是一种子字符串查找的算法，比起暴力查找，最大的优化是指向主串的指针不用回头，是递增的。而这一优化要归功于，子串（pattern）的完美前缀后缀算法，也就是网上大家所说的 next数组。next数组的存在，解决了当子串字符与主串字符不匹配时，子串指针应该回退的位置。

### 干货
虽说书读百遍其义自见，但是我觉得算法这东西单纯看书的话，很容易看着看着就放弃了，因为毕竟是翻译过来的，当然不是说这本书翻译得不好，只是翻译多多少少都会有偏差（或者是我蠢）。于是乎我在B站上找到了一位阿三（印度朋友）对于KMP的视频讲解，讲得很细很容易懂，[阿三的视频](https://www.bilibili.com/video/av3246487/?from=search&seid=1769270493014630067)

### show me the code

先实现next数组
```
public int[] next(char[] pattern){
    int l = pattern.length;
    int i = 0;
    int j = 1;
    int[] next = new int[l];
    for(;j < l;){
        if(pattern[i] == pattern[j]){
            next[j] = i + 1;
            j++;
            i++;

        }else{
            
            if(i == 0){
                next[j] = 0;
                j++;

            }else{
                i = next[i - 1];
            }

        }

    }
    return next;
}

```

真正的KMP
```
public boolean kmp(char[] txt,char[] pattern){
    
    int[] nn = next(pattern);
    int i = 0;
    int j = 0;

    while(i < txt.length && j < pattern.length){
        
        if(txt[i] == pattern[j]){
             i++; 
             j++;

        	}else{

        	   if(j != 0){	
                 j = nn[j - 1];
               }else{
                 i++;
             }

        	}
    }

    if(j == pattern.length){
        return true;
    }

    return false;
}

```











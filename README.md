[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/iHSjCEgj)
# J05
在先前的游戏的基础上添加了存档和回放的功能。每当启动一个新游戏时，将会开始录制一个以当前时间为名字的.jsonl文件
。所有的这样的文件会被保存在./recordings目录下，在menuScene中点击Replay即可选择和播放。

## 作业要求

- 参考本仓库代码，完善你自己的游戏：
 
- 为你的游戏设计并实现“存档与回放”功能：
  - 存档：定义存储抽象（文件/网络/内存均可），录制关键帧 + 输入/事件
  - 回放：读取存档，恢复对象状态并插值渲染，保证外观与行为可见且稳定

提示：请尽量保持模块解耦（渲染/输入/逻辑/存储）。

**重要提醒：尽量手写代码，不依赖自动生成，考试会考！**
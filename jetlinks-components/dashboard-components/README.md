# 仪表盘组件

获取所有类型

/dashboard/defs

    [
      {
          "id":"system",
          "name":"系统",
          "objects":[
            {
              "id":"memory",
              "name":"内存""
            },
            {
              "id":"cpu",
              "name":"CPU""
            }
          ]
      }
    ]


获取类型支持的指标和维度

/dashboard/类型/对象/指标

/dashboard/system/memory/measurements

    [
        {
         "id":"max",
         "name":"最大值",
         "dimensions":[ //指标支持的维度
           {"id":"time-interval","name":"历史数据","params":{"properties":[ ... ]}},
           {"id":"real-time","name":"实时数据","params":{"properties":[ ... ]}}
         ]
        },
        {
         "id":"usage",
         "name":"已使用"
        }
    ]



POST /dashboard/system/memory/_batch

    [
        {
           "measurement":"usage",
           "dimension":"time-interval",
           "params":{"interval":"1s"}
        },
        {
           "measurement":"max",
           "dimension":"time-interval",
           "params":{"interval":"1s"}
        }
    ]


    {
     "usage":[
        {
           "time":"15:00",
           "value":1.2
        },
        {
           "time":"16:00",
           "value":1.3
        }
     ]
     ,
     "max":[
         {
            "time":"15:00",
            "value":2
         },
         {
            "time":"16:00",
            "value":2
         }
      ]
    }

/dashboard/类型/对象/维度/指标?参数

GET /dashboard/device/property/temp/realTime?history=10
 
     {
        "timestamp":1578914267417,
        "value":5.6
     }
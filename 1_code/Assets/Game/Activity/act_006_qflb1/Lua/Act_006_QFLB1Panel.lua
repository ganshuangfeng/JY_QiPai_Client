local basefunc = require "Game/Common/basefunc"

Act_006_QFLB1Panel = basefunc.class()
local C = Act_006_QFLB1Panel
C.name = "Act_006_QFLB1Panel"

function C.Create(parent)
	return C.New(parent)
end

function C:AddMsgListener()
    for proto_name,func in pairs(self.lister) do
        Event.AddListener(proto_name, func)
    end
end

function C:MakeLister()
	self.lister = {}
	self.lister["AssetsGetPanelConfirmCallback"] = basefunc.handler(self,self.AssetsGetPanelConfirmCallback)

	self.lister["ExitScene"] = basefunc.handler(self,self.MyExit)
end

function C:RemoveListener()
    for proto_name,func in pairs(self.lister) do
        Event.RemoveListener(proto_name, func)
    end
    self.lister = {}
end

function C:MyExit()
	DSM.PopAct()
	self:RemoveListener()
	destroy(self.gameObject)

	 
end

function C:OnDestroy()
	self:MyExit()
end

function C:ctor(parent)

	ExtPanel.ExtMsg(self)

	DSM.PushAct({panel = C.name})
	local parent = parent or GameObject.Find("Canvas/GUIRoot").transform
	local obj = newObject(C.name, parent)
	local tran = obj.transform
	self.transform = tran
	self.gameObject = obj
	LuaHelper.GeneratingVar(self.transform, self)
	
	self:MakeLister()
	self:AddMsgListener()
	self:InitUI()
	
end

function C:InitUI()
	self.config = MoneyCenterQFLBManager.get_cfg()
	self.buy_btn.onClick:AddListener(
		function ()
			self:OnBuyClick(10084)
		end
	)
	self.get_btn.onClick:AddListener(
		function ()
			MoneyCenterQFLBPanel.Create()
		end
	)
	self.help_btn.onClick:AddListener(
		function ()
			LTTipsPrefab.Show(self.help_btn.gameObject.transform,2,self.config.qflb[1].desc)
		end
	)
	self:MyRefresh()
end

function C:MyRefresh()
	self.data = MoneyCenterQFLBManager.get_data_all_return_lb_info()
	local n = "all_return_lb_"
	local v = self.data[n .. 1]
	if v.is_buy == 1 then 
		self.get_btn.gameObject:SetActive(true)
		self.buy_btn.gameObject:SetActive(false)
	else	
		self.get_btn.gameObject:SetActive(false)
		self.buy_btn.gameObject:SetActive(true)
	end
end


function C:OnBuyClick(id)
	local gift_config = MainModel.GetShopingConfig(GOODS_TYPE.gift_bag, id)
	local status = MainModel.GetGiftShopStatusByID(gift_config.id)
    local b1 = MathExtend.isTimeValidity(gift_config.start_time, gift_config.end_time)
    if b1 then
		if status ~= 1 then
			LittleTips.Create("????????????????????????")
			return
		end
    else
		LittleTips.Create("???????????????????????????????????????")
		return
    end
    
	if GameGlobalOnOff.PGPay and gameRuntimePlatform == "Ios" then
		GameManager.GotoUI({gotoui = "sys_service_gzh",goto_scene_parm = "panel",desc="????????????????????????"})
	else
		PayTypePopPrefab.Create(gift_config.id, "???" .. (gift_config.price / 100))
	end
end

function C:AssetsGetPanelConfirmCallback(data)
	if data and data.change_type == "buy_gift_bag_10084" then
		Event.Brocast("Act_006_QFLB1_isbuy")
		--self:MyRefresh()
		MoneyCenterQFLBPanel.Create()
	end
end
local basefunc = require "Game/Common/basefunc"

Act_044_XNFDPanel = basefunc.class()
local M = Act_044_XNFDPanel
M.name = "Act_044_XNFDPanel"
local Mgr = Act_044_XNFDManager
local instance

local pay_fd = {
	[1] = 3,
	[2] = 5,
	[3] = 7, 
}

function M.Create(parent,cfg)
    if instance then
        instance:MyExit()
    end
    instance = M.New(parent,cfg)
	return instance
end

function M.Close()
    if instance then
        instance:MyExit()
    end
    instance = nil
end

function M:AddMsgListener()
	for proto_name,func in pairs(self.lister) do
		Event.AddListener(proto_name, func)
	end
end

function M:MakeLister()
	self.lister = {}
	self.lister["finish_gift_shop"] = basefunc.handler(self,self.on_finish_gift_shop)
	self.lister["act_044_xnfd_refresh"] = basefunc.handler(self,self.MyRefresh)
end

function M:RemoveListener()
	for proto_name,func in pairs(self.lister) do
		Event.RemoveListener(proto_name, func)
	end
	self.lister = {}
end

function M:MyExit()
	if self.Timer then
		self.Timer:Stop()
	end
	self:KillTween()
	self:RemoveListener()
    destroy(self.gameObject)
    instance = nil
end

function M:ctor(parent,cfg)
    ExtPanel.ExtMsg(self)
	local parent = parent or GameObject.Find("Canvas/GUIRoot").transform
	local obj = newObject(M.name, parent)
	local tran = obj.transform
	self.transform = tran
	self.gameObject = obj
	LuaHelper.GeneratingVar(self.transform, self)
	self.SV = self["Scroll View"].transform:GetComponent("ScrollRect")
	self:MakeLister()
	self:AddMsgListener() 
	self:InitUI()
	CommonTimeManager.GetCutDownTimer(cfg.endTime,self.cut_down_txt)
end

function M:InitUI()
	self:RefreshData()
	self.fd_objs = {}
	local ui_table = {}
	for i,v in ipairs(Mgr.gift_ids) do
		self.fd_objs[v] = newObject("Act_044_xnfd_fd",self.fd_content)
		ui_table = {}
		LuaHelper.GeneratingVar(self.fd_objs[v].transform, ui_table)
		ui_table.fd_img.sprite = GetTexture("gqfd_icon_" .. i)
		if self.gift_data[v].status == 0 then
			--礼包已购买
			ui_table.name_img.sprite = GetTexture("gqfd_btn_ylq_activity_act_044_xnfd")
		else
			ui_table.name_img.sprite = GetTexture("xnfd_btn_" .. i)
		end
		ui_table.name_btn = ui_table.name_img.transform:GetComponent("Button")
		local gift_id = v
		ui_table.name_btn.onClick:AddListener(function()
			self.selected_gift = gift_id
			self:RefreshGift()
		end)
		if i % 2 == 0 then
			ui_table.fd_img.transform.localPosition = Vector3.New(0,-80,0)
		end
	end
	ui_table = nil
	self.selected_gift = Mgr.gift_ids[1] --选中礼包id
	for i,v in ipairs(Mgr.gift_ids or {}) do
		if self.gift_data[v] and self.gift_data[v].status == 1 then
			self.selected_gift = v
			--可以购买
			break
		end
	end
	dump(self.selected_gift,"3333333333333333")
	self.selected_task = 1
	for i,v in ipairs(self.task_atas or {}) do
		if v == 1 then
			self.selected_task = i
			break
		end
	end

	for i=1,3 do
		self[i .. "_tge"].onValueChanged:AddListener(
			function(val)
				self[i .. "_Label"].gameObject:SetActive(val)
				ExtendSoundManager.PlaySound(audio_config.game.com_but_confirm.audio_name)
				if val then
					self.selected_task = i
					self:RefreshTask()
				end
			end
		)
	end
	self.get_btn.onClick:AddListener(function(  )
		if not self.selected_task or not Mgr.task_id then return end
		Network.SendRequest("get_task_award_new", { id = Mgr.task_id, award_progress_lv = tonumber(self.selected_task) })
	end)

	self.box_btn.onClick:AddListener(function(  )
		if not self.selected_task or not Mgr.task_id then return end
		Network.SendRequest("get_task_award_new", { id = Mgr.task_id, award_progress_lv = tonumber(self.selected_task) })
	end)
	self.box_ani = self.box_img.transform:GetComponent("Animator")

	self.pay_btn.onClick:AddListener(function(  )
		if not self.selected_gift then return end
		self:BuyShop(self.selected_gift)
	end)
	self.not_pay_btn.onClick:AddListener(function(  )
		LittleTips.Create("您今日已购买过该礼包，请于明日购买")
	end)
	self:MyRefresh()
	self.SV.horizontalNormalizedPosition = 1
	self:Anim()
end

function M:Anim()
	if self:GetGiftIndex() <= 2 then
		self.Timer = Timer.New(
			function()
				self.SV.horizontalNormalizedPosition = self.SV.horizontalNormalizedPosition - 0.02
			end,0.02,50
		)
		self.Timer:Start()
	end
end

function M:MyRefresh()
	self.selected_task = 1
	for i,v in ipairs(self.task_atas or {}) do
		if v ~= 2 then
			self.selected_task = i
			break
		end
	end
	self:RefreshTask()
	for i,v in ipairs(Mgr.gift_ids or {}) do
		if self.gift_data[v] and self.gift_data[v].status == 1 then
			self.selected_gift = v
			--可以购买
			break
		end
	end
	local gift_index = self:GetGiftIndex()
	dump(gift_index,"<color=red>当前位置——————————————</color>")
	self.SV.horizontalNormalizedPosition = gift_index /7
	self:RefreshData()
	self:RefreshGift()
end

function M:OnDestroy()
	self:MyExit()
end

function M:GetGiftIndex()
	dump(Mgr.gift_ids)
	dump(self.selected_gift)
	for i = 1,#Mgr.gift_ids do
		if Mgr.gift_ids[i] == self.selected_gift then
			return i
		end
	end
	return 1
end

function M:RefreshGift()
	if not self.selected_gift then return end
	local gift_cfg = self.gift_cfg[self.selected_gift]
	local gift_data = self.gift_data[self.selected_gift]
	local fd_obj = self.fd_objs[self.selected_gift]
	if not gift_cfg or not gift_data or not IsEquals(fd_obj) then
		gift_cfg = nil
		gift_data = nil
		fd_obj = nil
		return
	end

	local ui_table = {}
	for k,v in pairs(self.fd_objs or {}) do
		ui_table = {}
		LuaHelper.GeneratingVar(v.transform, ui_table)
		if k == self.selected_gift then
			--选中
			ui_table.tx_node.gameObject:SetActive(true)
			self:CurPanelShake(ui_table.fd_img.gameObject, 0.3)
		else
			ui_table.tx_node.gameObject:SetActive(false)
		end

		if self.gift_data[k].status == 0 then
			--礼包已购买
			ui_table.name_img.sprite = GetTexture("gqfd_btn_ylq_activity_act_044_xnfd")
		end
	end

	self.price_txt.text = gift_cfg.price / 100 .. "元购买"
	self.not_pay_btn.gameObject:SetActive(gift_data.status ~= 1)

	if self.cur_selected_gift and self.cur_selected_gift == self.selected_gift then return end
	self.cur_selected_gift = self.selected_gift
	--奖励刷新
	destroyChildren(self.jl_content)
	local item
	local img
	local obj
	local count
	for i,v in ipairs(gift_cfg.buy_asset_type) do
		ui_table = {}
		obj = newObject("Act_044_xnfd_jl",self.jl_content)
		LuaHelper.GeneratingVar(obj.transform, ui_table)
		count = gift_cfg.buy_asset_count[i]
		if v == "jing_bi" then
			if count < 180000 then
				img = "pay_icon_gold3"
			else
				img = "pay_icon_gold4"
			end
			ui_table.icon_img.sprite = GetTexture(img)
			ui_table.num_txt.text = StringHelper.ToCash(count)
		else
			item = GameItemModel.GetItemToKey(v)
			if not table_is_null(item) then
				ui_table.icon_img.sprite = GetTexture(item.image)
				ui_table.icon_btn = ui_table.icon_img.transform:GetComponent("Button")
				local desc = item.desc
				ui_table.icon_btn.onClick:AddListener(function(  )
					LittleTips.Create(desc)
				end)
			end
			ui_table.num_txt.text = StringHelper.ToCash(count)
		end
	end
	item = nil
	img = nil
	obj = nil
	ui_table = nil
end

function M:RefreshTask()
	if not self.selected_task then return end
	if not self.task_data then 
		self.task_data = Mgr.GetTaskData()
	end
	if not self.task_data then return end
	for i=1,3 do
		self[i .. "_tge"].isOn = i == self.selected_task
	end
	self.task_atas = Mgr.GetAllTaskAwardStatus()
	if not table_is_null(self.task_atas) then
		if self.task_atas[self.selected_task] == 0 then
			self.get_txt.text = "任购<color=#fffd45>" .. pay_fd[self.selected_task] .. "</color>个福袋\n获得额外奖励"
			self.get_btn.gameObject:SetActive(false)
			self.box_btn.gameObject:SetActive(false)
			self.box_ani.enabled = false
		elseif self.task_atas[self.selected_task] == 1 then
			self.get_txt.text = ""
			self.get_btn.gameObject:SetActive(true)
			self.box_btn.gameObject:SetActive(true)
			self.box_ani.enabled = true
		elseif self.task_atas[self.selected_task] == 2 then
			self.get_txt.text = "已领取"
			self.get_btn.gameObject:SetActive(false)
			self.box_btn.gameObject:SetActive(false)
			self.box_ani.enabled = false
		end
	end
	local i = self.selected_task + 7
	self.box_img.sprite = GetTexture("gqfd_icon_" .. i)
	if self.selected_task == 1 then
		self.box_txt.text = "最高20万鲸币"
	elseif self.selected_task == 2 then
		self.box_txt.text = "最高50万鲸币"
	elseif self.selected_task == 3 then
		self.box_txt.text = "最高100万鲸币"
	end
end

function M:BuyShop(shopid)
    local price = MainModel.GetShopingConfig(GOODS_TYPE.gift_bag, shopid).price
    dump(MainModel.GetShopingConfig(GOODS_TYPE.gift_bag, shopid))
    if  GameGlobalOnOff.PGPay and gameRuntimePlatform == "Ios" then
        ServiceGzhPrefab.Create({desc="请前往公众号获取"})
    else
        PayTypePopPrefab.Create(shopid, "￥" .. (price / 100))
    end
end

function M:RefreshData()
	self.gift_data = {}
	self.gift_cfg = {}
	for i,v in ipairs(Mgr.gift_ids) do
		self.gift_cfg[v] = MainModel.GetShopingConfig(GOODS_TYPE.gift_bag,v)
		self.gift_data[v] = MainModel.GetGiftDataByID(v)
		if not self.gift_data[v] then
			self.gift_data[v] = {
				status = 0
			}
		end
	end
	self.task_data = Mgr.GetTaskData()
	self.task_atas = Mgr.GetAllTaskAwardStatus()
end

function M:CurPanelShake(obj, t ,k)
	self:KillTween()
  	t = t or 1
    self.seq = DoTweenSequence.Create()
    self.seq:Append(obj.transform:DOShakePosition(t, Vector3.New(10, 10, 0), 20))
    self.seq:OnKill(function()
			-- obj.transform.localPosition = Vector3.zero
    	end)
  	self.seq:OnForceKill(function ()
		-- obj.transform.localPosition = Vector3.zero
	end)
end

function M:KillTween()
	if self.seq then
		self.seq:Kill()
	end
	self.seq = nil
end

function M:on_finish_gift_shop()
	
end
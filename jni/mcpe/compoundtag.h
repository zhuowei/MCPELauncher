struct TagMemoryChunk {
	std::string contents;
	int type;
};

class Tag {
};

class CompoundTag: public Tag {
public:

int getId() const
void write(IDataOutput&) const
bool equals(Tag const&) const
std::string toString() const
void print(std::string const&, PrintStream&) const
CompoundTag()
CompoundTag(std::string const&)
CompoundTag(CompoundTag&&)
Tag* get(std::string const&) const
bool contains(std::string const&) const
bool contains(std::string const&, int) const
int8_t getByte(std::string const&) const
int16_t getShort(std::string const&) const
int getInt(std::string const&) const
int64_t getInt64(std::string const&) const
float getFloat(std::string const&) const
double getDouble(std::string const&) const
std::string getString(std::string const&) const
TagMemoryChunk getByteArray(std::string const&) const
TagMemoryChunk getIntArray(std::string const&) const
CompoundTag* getCompound(std::string const&) const
ListTag* getList(std::string const&) const
bool getBoolean(std::string const&) const
bool isEmpty() const
void* rawView() const
operator=(CompoundTag&&)
~CompoundTag()
~CompoundTag()
void getAllTags(std::vector<Tag*, std::allocator<Tag*> >&) const
void remove(std::string const&)
void load(IDataInput&)
void put(std::string const&, std::unique_ptr<Tag, std::default_delete<Tag> >)
CompoundTag* copy() const
CompoundTag* clone() const
std::unique_ptr<CompoundTag> uniqueClone() const
void putByte(std::string const&, char)
void putBoolean(std::string const&, bool)
void putShort(std::string const&, short)
void putInt(std::string const&, int)
void putInt64(std::string const&, long long)
void putFloat(std::string const&, float)
void putDouble(std::string const&, float)
void putString(std::string const&, std::string const&)
void putByteArray(std::string const&, TagMemoryChunk)
void putCompound(std::string const&, std::unique_ptr<CompoundTag, std::default_delete<CompoundTag> >)

